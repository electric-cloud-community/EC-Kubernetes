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

use ElectricCommander;

my $ec = ElectricCommander->new();
my $config = '$[config]';

if (!defined $config || $config eq "" ) {
    my $errMsg = "config parameter must exist and be non-blank";
    print $errMsg . "\n";
    $ec->setProperty("/myJob/configError", $errMsg);
    exit 1;
}

$ec->deleteProperty("/myProject/ec_plugin_cfgs/$config");
$ec->deleteCredential('$[/myProject/projectName]', $config);

exit 0;