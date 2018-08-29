#
#  Copyright 2016 Electric Cloud, Inc.
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#

=head1 NAME

retrieveGrapeDependencies.pl

=head1 DESCRIPTION


Retrieves artifacts published as artifact EC-Docker-Grapes
to the grape root directory configured with ec-groovy.

=head1 METHODS

=cut

use File::Copy::Recursive qw(rcopy);
use File::Path;
use ElectricCommander;
use Digest::MD5 qw(md5_hex);
use MIME::Base64;
use File::Temp qw(tempfile tempdir);
use Archive::Zip;

use warnings;
use strict;
$|=1;


$::gAdditionalArtifactVersion = "$[additionalArtifactVersion]";

sub main() {
    my $ec = ElectricCommander->new({timeout => 300});
    $ec->abortOnError(1);

    parseProperties($ec);

    my $resource = $ec->getProperty('/myJobStep/assignedResourceName')->findvalue('//value')->string_value;
    $ec->setProperty({propertyName => '/myJob/grabbedResource', value => $resource});
    print "Grabbed Resource: $resource\n";

    if ($::gAdditionalArtifactVersion ne '') {
#        For other plugins
        retrieveGrapeDependency($ec, $::gAdditionalArtifactVersion);
    }
}

########################################################################
# retrieveGrapeDependency - Retrieves the artifact version and copies it
# to the grape directory used by ec-groovy for @Grab dependencies
#
# Arguments:
#   -ec
#   -artifactVersion
########################################################################
sub retrieveGrapeDependency($){
    my ($ec, $artifactVersion) = @_;

    my $xpath = $ec->retrieveArtifactVersions({
        artifactVersionName => $artifactVersion
    });

    # copy to the grape directory ourselves instead of letting
    # retrieveArtifactVersions download to it directly to give
    # us better control over the over-write/update capability.
    # We want to copy only files the retrieved files leaving
    # the other files in the grapes directory unchanged.
    my $dataDir = $ENV{COMMANDER_DATA};
    die "ERROR: Data directory not defined!" unless ($dataDir);

    my $grapesDir = $ENV{COMMANDER_DATA} . '/grape';
    my $dir = $xpath->findvalue("//artifactVersion/cacheDirectory");

    mkpath($grapesDir);
    die "ERROR: Cannot create target directory" unless( -e $grapesDir );

    rcopy( $dir, $grapesDir) or die "Copy failed: $!";
    print "Retrieved and copied grape dependencies from $dir to $grapesDir\n";

    my $resource = $ec->getProperty('/myJobStep/assignedResourceName')->findvalue('//value')->string_value;
    $ec->setProperty({propertyName => '/myJob/grabbedResource', value => $resource});
    print "Grabbed Resource: $resource\n";

}

sub retrieveBase64Properties {
    my $dependenciesProperty = '/projects/@PLUGIN_NAME@/ec_groovyDependencies';
    my $ec = ElectricCommander->new;

    my $hasNext = 1;
    my $chunkNumber = 0;
    my $checksum = $ec->getProperty("$dependenciesProperty/checksum")->findvalue('//value')->string_value;
    my $base64 = '';
    while($hasNext) {
        eval {
            my $chunk = $ec->getProperty($dependenciesProperty . "/ec_dependencyChunk_" . $chunkNumber)->findvalue('//value')->string_value;
            $base64 .= $chunk;
            $chunkNumber ++;
            print "Found chunk $chunkNumber\n";
            1;
        } or do {
            $hasNext = 0;
        };
    }

    if ($checksum ne md5_hex($base64)) {
        die "Checksums do not match!";
    }
    return $base64;
}

sub parseProperties {
    my ($commander) = @_;

    my $base64 = retrieveBase64Properties();
    my $binary = decode_base64($base64);
    my ($tempFh, $tempFilename) = tempfile(CLEANUP => 1);
    binmode($tempFh);
    print $tempFh $binary;
    close $tempFh;

    my $zip = Archive::Zip->new();
    unless($zip->read($tempFilename) == Archive::Zip::AZ_OK()) {
        die "Cannot read .zip dependencies: $!";
    }

    my $grapesDir = $ENV{COMMANDER_DATA} . '/grape';
    mkpath($grapesDir);

    $zip->extractTree("lib", $grapesDir . '/' );
    print "Downloaded and extracted\n";
}

main();

1;
