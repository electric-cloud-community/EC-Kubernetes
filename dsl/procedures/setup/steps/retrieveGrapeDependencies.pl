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


package EC::DependencyManager;
use strict;
use warnings;

use File::Spec;
use JSON qw(decode_json encode_json);
use File::Copy::Recursive qw(rcopy rmove);
use File::Find;
use Digest::MD5;
use Data::Dumper;
use subs qw(info debug);

our $saveChecksumSub;

sub new {
    my ($class, $ec, %options) = @_;

    my $self = { ec => $ec };
    $self->{dest} = $options{destination} || "$ENV{COMMANDER_DATA}/grape";

    # Rather strange way of declaring the subroutine, it has to be accessible from the spawned step and from the main code
    $saveChecksumSub = q{

sub doSaveChecksum {
    my ($ec, $folder, $project) = @_;

    my $digest = Digest::MD5->new;
    my @files = ();
    find({wanted => sub {
        if (-f $File::Find::name) {
            push @files, File::Spec->abs2rel($File::Find::name, $folder);
        }
    }, no_chdir => 1}, $folder);

    @files = sort @files;
    for my $file (@files) {
        my $filename = File::Spec->catfile($folder, $file);
        next if $filename =~ /ivydata/;
        open my $fh, $filename or die "Cannot open $filename: $!";
        binmode $fh;
        my $content = join('', <$fh>);
        close $fh;
        $digest->add($content);
    }

    # Relative to grape/ folder
    my $checksums = {files => \@files, checksum => $digest->hexdigest};
    $ec->setProperty("/projects/$project/ec_dependencies", encode_json($checksums), {description => 'List of dependencies files and checksum'});
}

    };

    return bless $self, $class;
}


sub ec {
    return shift->{ec};
}

sub destination {
    return shift->{dest};
}

sub grabResource {
    my ($self) = @_;

    my $resName = '$[/myResource/resourceName]';
    $self->ec->setProperty('/myJob/grabbedResource', $resName);
    print "Grabbed Resource: $resName\n";
}





# Getting the name of the server ("local") resource either from server settings or looking for it in resources
sub getLocalResource {
    my ($self) = @_;

    my @filterList = ();

    my $propertyPath = '/server/settings/localResourceName';
    my $serverResource = eval {
        $self->ec->getProperty($propertyPath)->findvalue('//value')->string_value
    };

    if ($serverResource) {
        print "Configured Local Resource is $serverResource (taken from $propertyPath)\n";
        return $serverResource;
    }

    push (@filterList, {"propertyName" => "hostName",
                            "operator" => "equals",
                            "operand1" => "localhost"});

    push (@filterList, {"propertyName" => "hostName",
                            "operator" => "equals",
                            "operand1" => "127.0.0.1"});

    my $hostname = $self->ec->getProperty('/server/hostName')->findvalue('//value')->string_value;
    print "Hostname is $hostname\n";

    push (@filterList, {"propertyName" => "hostName",
                            "operator" => "equals",
                            "operand1" => "$hostname"});

    push (@filterList, {"propertyName" => "resourceName",
                            "operator" => "equals",
                            "operand1" => "local"});

    my $result = $self->ec->findObjects('resource',
            {filter => [
         { operator => 'or',
             filter => \@filterList,
        }
      ], numObjects => 1}
    );

    my $resourceName = eval {
        $result->findvalue('//resourceName')->string_value;
    };
    unless($resourceName) {
        die "Cannot find local resource and there is no local resource in the configuration. Please set property $propertyPath to the name of your local resource or resource pool.";
    }
    print "Found local resource: $resourceName\n";
    return $resourceName;
}

sub copyDependencies {
    my ($self, $projectName) = @_;

    my $source = $self->getPluginsFolder() . "/$projectName/lib";
    $self->saveChecksum($source, $projectName);
    my $dest = $self->destination;
    unless(-d $source) {
        die "Folder $source does not exist, please try to reinstall the plugin."
    }

    unless(-d $dest) {
        mkdir($dest);
    }

    unless( -w $dest) {
        die "$dest is not writable. Please allow agent user to write to this directory."
    }

    my $filesCopied = rcopy($source, $dest);
    if ($filesCopied == 0) {
        die "Copy failed, no files were copied from $source to $dest, please check permissions for $dest";
    }
}

sub getPluginsFolder {
    my ($self) = @_;

    return $self->ec->getProperty('/server/settings/pluginsDirectory')->findvalue('//value')->string_value;
}

sub sendDependencies {
    my ($self, @projects) = @_;

    my $checksumsOk = 1;
    for my $project (@projects) {
        unless($self->checkChecksums($project)) {
            $checksumsOk = 0;
        }
    }

    if ($checksumsOk) {
        info "Dependencies cache is ok, no dependency transfer is required";
        $self->setSummary("Dependencies will be taken from the local cache");
        return 0;
    }

    my $serverResource = $self->getLocalResource();
    my $currentResource = '$[/myResource/resourceName]';
    if ($serverResource eq $currentResource) {
        for my $projectName (@projects) {
            $self->copyDependencies($projectName);
        }
        return;
    }

    my $grapeFolder = $self->destination;
    my $windows = $^O =~ /win32/i;

    my $channel = int rand 9999999;

    my $pluginsFolder = $self->getPluginsFolder;
    my @folders = map {
        $pluginsFolder . '/' . $_;
    } @projects;

    my $sendStep = q{
use strict;
use warnings;
use ElectricCommander;
use JSON qw(encode_json);
use Data::Dumper;
use File::Find;
use Digest::MD5;
use File::Basename qw(basename);

my $pluginFolders = '#pluginFolders#';
my @folders = split(';', $pluginFolders);

my $ec = ElectricCommander->new;
my $channel = '#channel#';
print "Channel: $channel\n";

checkStompPort();

my %mapping = ();
for my $folder (@folders) {
    my $project = basename($folder);
    $folder = File::Spec->catfile($folder, 'lib');
    unless(-d $folder) {
        handleError("Folder $folder does not exist");
    }
    my @files = scanFiles($folder);


    for my $file (@files) {
        my $relPath = File::Spec->abs2rel($file, $folder);
        my $destPath = "grape/$relPath";
        $mapping{$file} = $destPath;
    }
    doSaveChecksum($ec, $folder, $project);
}

my $response = $ec->putFiles($ENV{COMMANDER_JOBID}, \%mapping, {channel => $channel});
$ec->setProperty('/myJob/ec_dependencies_files', encode_json(\%mapping));

sub handleError {
    my ($error) = @_;

    print 'Error: ' . $error;
    $ec->setProperty('/myJobStep/summary', $error);
    exit 1;
}

sub scanFiles {
    my ($dir) = @_;

    my @files = ();
    opendir my $dh, $dir or handleError("Cannot open folder $dir: $!");
    for my $file (readdir $dh) {
        next if $file =~ /^\./;

        my $fullPath = File::Spec->catfile($dir, $file);
        if (-d $fullPath) {
            push @files, scanFiles($fullPath);
        }
        else {
            push @files, $fullPath;
        }
    }
    return @files;
}

#saveChecksum#

sub checkStompPort {
    my $stomp = $ec->getServerInfo()->findvalue('//stompClientUri')->string_value;
    my $failed = 0;

    my ($host, $port) = $stomp =~ m/:\/\/([\w+.-]+):(\d+)/;
    require IO::Socket::INET;
    my $checkport = IO::Socket::INET->new(
              PeerAddr => "$host",
              PeerPort => "$port",
              Proto => 'tcp',
              Timeout => '0')
    or $failed = 1;

    if ($failed) {
        print "[WARNING] cannot connect to STOMP server at $host:$port\n";
    }
    else {
        print "STOMP server is accessible at $host:$port\n";
    }

}
    };

    my $pluginFolders = join(';', @folders);
    $sendStep =~ s/\#pluginFolders\#/$pluginFolders/;
    $sendStep =~ s/\#channel\#/$channel/;
    $sendStep =~ s/\#saveChecksum\#/$saveChecksumSub/;

    $self->checkStomp;

    my $xpath = $self->ec->createJobStep({
        jobStepName => 'Grab Dependencies',
        command => $sendStep,
        shell => 'ec-perl',
        resourceName => $serverResource
    });

    my $jobStepId = $xpath->findvalue('//jobStepId')->string_value;
    info "Spawned job step for collecting dependencies: $jobStepId";
    my $completed = 0;
    while(!$completed) {
        my $status = $self->ec->getJobStepStatus($jobStepId)->findvalue('//status')->string_value;
        if ($status eq 'completed') {
            $completed = 1;
        }
    }

    my $err;
    my $timeout = 60;
    $self->ec->getFiles({error => \$err, channel => $channel, timeout => $timeout});
    if ($err) {
        die $err;
    }
    my $files = eval {
        $self->ec->getProperty('/myJob/ec_dependencies_files')->findvalue('//value')->string_value;
    };
    if ($@) {
        die "Cannot get property ec_dependencies_files from the job: $@, please try to rerun the job";
    }

    my $mapping = decode_json($files);
    for my $file (keys %$mapping) {
        my $dest = $mapping->{$file};
        if (-f $dest) {

        }
        else {
            die "The dependency file $dest was sent but not received, please try to rerun the job\n";
        }
    }

    unless(-d $grapeFolder) {
        mkdir $grapeFolder;
    }

    unless( -w $grapeFolder) {
        die "$grapeFolder is not writable. Please allow agent user to write to this directory."
    }
    my $filesCopied = rmove('grape', $grapeFolder);
    if ($filesCopied == 0) {
        die "Copy failed, no files were copied to $grapeFolder, please check permissions for the directory $grapeFolder";
    }
    info "Received dependencies";
}

sub printLog {
    my ($marker, @messages) = @_;

    for my $message (@messages) {
        if (ref $message) {
            $message = Dumper($message);
        }
        print "$marker $message\n";
    }
}


sub debug {
    my @messages = @_;

    if ($ENV{DEPENDENCIES_DEBUG}) {
        printLog('[DEBUG]', @messages);
    }
}


sub info {
    my @messages = @_;

    printLog('[INFO]', @messages);
}


sub checkChecksums {
    my ($self, $project) = @_;

    my $deps = eval {
        my $string = $self->ec->getProperty("/projects/$project/ec_dependencies")->findvalue('//value')->string_value;
        decode_json($string);
    };

    return 0 unless $deps;
    return 0 unless $deps->{files};
    return 0 unless $deps->{checksum};

    my $digest = Digest::MD5->new;

    for my $file (@{$deps->{files}}) {
        my $filename = File::Spec->catfile($self->destination, $file);
        next if $filename =~ /ivydata/; # this one changes
        open my $fh, $filename or return 0;
        binmode $fh;
        my $content = join('', <$fh>);
        close $fh;
        $digest->add($content);
    }

    my $checksum = $digest->hexdigest;
    if ($checksum ne $deps->{checksum}) {
        info "Checksums do not match: $deps->{checksum} and $checksum\n";
        return 0;
    }
    return 1;
}


sub saveChecksum {
    my ($self, $folder, $project) = @_;

    no warnings 'redefine';

    eval $saveChecksumSub;
    if (@$) {
        info "Cannot call saveChecksum: $@";
        return;
    }
    # Declared in the variable
    # This was done to remove duplicates
    doSaveChecksum($self->ec, $folder, $project);
}

sub setSummary {
    my ($self, $summary) = @_;

    $self->ec->setProperty('/myJobStep/summary', $summary);
}


sub checkStomp {
    my ($self) = @_;

    my $uri = $self->ec->getServerInfo()->findvalue('//stompClientUri')->string_value;
    info "STOMP URI is $uri, it should be accessible from the agent machine";
}

1;

