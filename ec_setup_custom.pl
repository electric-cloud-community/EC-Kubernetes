# Plugin-specific setup code
my $setup = ECSetup->new(
    commander => $commander,
    pluginName => $pluginName,
    otherPluginName => $otherPluginName,
    upgradeAction => $upgradeAction,
    promoteAction => $promoteAction,
);
$setup->promotePlugin([
    {artifactName => '@PLUGIN_KEY@-Grapes', artifactVersion => '1.0.2', fromDirectory => 'lib/grapes'}
]);

# ec_setup.pl shared code
#####################################
package ECSetup;

use strict;
use warnings;

use File::Spec;
use Archive::Zip;
use MIME::Base64;
use Digest::MD5 qw(md5_hex);
use File::Temp qw(tempfile tempdir);
use Cwd;
use POSIX;


sub new {
    my ($class, %param) = @_;

    my $self = { %param };
    $self->{commander} or die '$commander object should be provided!';
    $self->{promoteAction} or die '$promoteAction should be provided!';
    defined $self->{upgradeAction} or die '$upgradeAction should be provided!';
    $self->{pluginName} or die '$pluginName should be provided!';
    defined $self->{otherPluginName} or die '$otherPluginName should be provided!';

    return bless $self, $class;
}

sub commander { shift->{commander} }
sub promoteAction { shift->{promoteAction} }
sub upgradeAction { shift->{upgradeAction} }
sub pluginName { shift->{pluginName} }
sub otherPluginName { shift->{otherPluginName} }

sub publishArtifact {
    my ($self, $artifactName, $artifactVersion, $fromDirectory) = @_;

    $artifactName or die 'Artifact name should be provided!';
    $artifactVersion or die 'Artifact version should be provided!';
    $fromDirectory or die 'fromDirectory should be provided!';

    # This is here because we cannot do publishArtifactVersion in dsl today
    # delete artifact if it exists first
    my $commander = $self->commander;
    $commander->deleteArtifactVersion("com.electriccloud:$artifactName:$artifactVersion");

    my $dependenciesProperty = '/projects/@PLUGIN_NAME@/ec_groovyDependencies';
    my $base64 = '';
    my $xpath;
    eval {
      $xpath = $commander->getProperties({path => $dependenciesProperty});
      1;
    };
    unless($@) {
      my $blocks = {};
      my $checksum = '';
      for my $prop ($xpath->findnodes('//property')) {
        my $name = $prop->findvalue('propertyName')->string_value;
        my $value = $prop->findvalue('value')->string_value;
        if ($name eq 'checksum') {
          $checksum = $value;
        }
        else {
          my ($number) = $name =~ /ec_dependencyChunk_(\d+)$/;
          $blocks->{$number} = $value;
        }
      }
      for my $key (sort {$a <=> $b} keys %$blocks) {
        $base64 .= $blocks->{$key};
      }

      my $resultChecksum = md5_hex($base64);
      unless($checksum) {
        die "No checksum found in dependendencies property, please reinstall the plugin";
      }
      if ($resultChecksum ne $checksum) {
        die "Wrong dependency checksum: original checksum is $checksum";
      }
    }

    return unless $base64;

    my $binary = decode_base64($base64);
    my ($tempFh, $tempFilename) = tempfile(CLEANUP => 1);
    binmode($tempFh);
    print $tempFh $binary;
    close $tempFh;

    my ($tempDir) = tempdir(CLEANUP => 1);
    my $zip = Archive::Zip->new();
    unless($zip->read($tempFilename) == Archive::Zip::AZ_OK()) {
      die "Cannot read .zip dependencies: $!";
    }
    $zip->extractTree("", $tempDir . '/');

    my $logfile = '';
    if ( $self->promoteAction eq "promote" ) {
        #publish jars to the repo server if the plugin project was created successfully
        my $am = new ElectricCommander::ArtifactManagement($commander);
        my $artifactVersion = $am->publish(
            {   groupId         => "com.electriccloud",
                artifactKey     => "$artifactName",
                version         => $artifactVersion,
                includePatterns => "**",
                fromDirectory   => File::Spec->catfile($tempDir, $fromDirectory),
                description => 'JARs that @PLUGIN_NAME@ plugin procedures depend on'
            }
        );

        # Print out the xml of the published artifactVersion.
        $logfile .= $artifactVersion->xml() . "\n";

        if ( $artifactVersion->diagnostics() ) {
            $logfile .= "\nDetails:\n" . $artifactVersion->diagnostics();
        }
    }

    return $logfile;
}

sub promotePlugin {
    my ($self, $dependencies) = @_;

    my $dir = getcwd;
    my $logfile = "";
    my $pluginDir;

    my $commander = $self->commander;
    my $pluginName = $self->pluginName;

    if ($dependencies) {
        for my $dependency (@$dependencies) {
            $logfile .= $self->publishArtifact($dependency->{artifactName}, $dependency->{artifactVersion}, $dependency->{fromDirectory});
        }
    }
}
