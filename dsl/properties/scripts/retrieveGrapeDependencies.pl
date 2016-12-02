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


Retrieves artifacts published as artifact EC-GoogleContainerEngine-Grapes
to the grape root directory configured with ec-groovy.

=head1 METHODS

=cut

use strict;
use warnings;

use File::Copy::Recursive qw(rcopy);
use File::Path;

use ElectricCommander;

$|=1;

main();

sub main {
    my $ec = ElectricCommander->new();
    $ec->abortOnError(1);

    my $xpath = $ec->retrieveArtifactVersions({
        artifactVersionName => 'com.electriccloud:EC-GoogleContainerEngine-Grapes:1.0.0'
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
    print "Retrieved and copied grape dependencies to $grapesDir";

}