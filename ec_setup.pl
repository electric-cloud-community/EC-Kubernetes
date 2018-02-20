use Cwd;
use File::Spec;
use POSIX;
use Archive::Zip;
use MIME::Base64;
use Digest::MD5 qw(md5_hex);
use File::Temp qw(tempfile tempdir);
my $dir = getcwd;
my $logfile ="";
my $pluginDir;

if ( defined $ENV{QUERY_STRING} ) {    # Promotion through UI
    $pluginDir = $ENV{COMMANDER_PLUGINS} . "/$pluginName";
}
else {
    my $commanderPluginDir = $commander->getProperty('/server/settings/pluginsDirectory')->findvalue('//value');
    $pluginDir = File::Spec->catfile($commanderPluginDir, $pluginName);
}

$logfile .= "Plugin directory is $pluginDir\n";

$commander->setProperty("/plugins/$pluginName/project/pluginDir", {value=>$pluginDir});
$logfile .= "Plugin Name: $pluginName\n";
$logfile .= "Current directory: $dir\n";

# Evaluate promote.groovy or demote.groovy based on whether plugin is being promoted or demoted ($promoteAction)
local $/ = undef;
my $demoteDsl = q{
# demote.groovy placeholder
};

my $promoteDsl = q{
# promote.groovy placeholder
};

my $dsl;
if ($promoteAction eq 'promote') {
  $dsl = $promoteDsl;
}
else {
  $dsl = $demoteDsl;
}

my $dslReponse = $commander->evalDsl(
    $dsl, {
        parameters => qq(
                     {
                       "pluginName":"$pluginName",
                       "upgradeAction":"$upgradeAction",
                       "otherPluginName":"$otherPluginName"
                     }
              ),
        debug             => 'false',
        serverLibraryPath => File::Spec->catdir( $pluginDir, 'dsl' ),
    },
);


$logfile .= $dslReponse->findnodes_as_string("/");

my $errorMessage = $commander->getError();
if ( !$errorMessage ) {

    # This is here because we cannot do publishArtifactVersion in dsl today
    # delete artifact if it exists first
    $commander->deleteArtifactVersion("com.electriccloud:EC-Kubernetes-Grapes:1.0.0");

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
}

# Create output property for plugin setup debug logs
my $nowString = localtime;
$commander->setProperty( "/plugins/$pluginName/project/logs/$nowString", { value => $logfile } );

die $errorMessage unless !$errorMessage
