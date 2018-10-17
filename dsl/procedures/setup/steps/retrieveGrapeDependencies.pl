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

use warnings;
use strict;
$|=1;


$::gAdditionalArtifactVersion = "$[additionalArtifactVersion]";

sub main() {
    my $ec = ElectricCommander->new();
    $ec->abortOnError(1);

    my $pluginName = eval {
        $ec->getProperty('additionalPluginName')->findvalue('//value')->string_value
    };
    my @projects = ();
    push @projects, '$[/myProject/projectName]';

    if ($pluginName) {
        # This is a new one
        my @names = split(/\s*,\s*/, $pluginName);
        for my $name (@names) {
            my $projectName = $ec->getPlugin($pluginName)->findvalue('//projectName')->string_value;
            push @projects, $projectName;
        }
    }

    retrieveDependencies($ec, @projects);

    # This part remains as is
    if ($::gAdditionalArtifactVersion ne '') {
        my @versions = split(/\s*,\s*/, $::gAdditionalArtifactVersion);
        for my $version (@versions) {
            retrieveGrapeDependency($ec, $version);
        }
    }
}


sub retrieveDependencies {
    my ($ec, @projects) = @_;

    my $dep = EC::DependencyManager->new($ec);
    $dep->grabResource();
    eval {
        $dep->sendDependencies(@projects);
    };
    if ($@) {
        my $err = $@;
        print "$err\n";
        $ec->setProperty('/myJobStep/summary', $err);
        exit 1;
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

    my $grapesDir = $ENV{COMMANDER_DATA} . '/grape/grapes';
    my $dir = $xpath->findvalue("//artifactVersion/cacheDirectory");

    mkpath($grapesDir);
    die "ERROR: Cannot create target directory" unless( -e $grapesDir );

    rcopy( $dir, $grapesDir) or die "Copy failed: $!";
    print "Retrieved and copied grape dependencies from $dir to $grapesDir\n";
}

main();

1;


$[/myProject/scripts/EC/DependencyManager]
