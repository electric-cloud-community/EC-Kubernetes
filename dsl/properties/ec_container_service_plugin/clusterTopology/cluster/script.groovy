def projectName = args.projectName
def environmentName = args.environmentName
def clusterName = args.clusterName
def configParameters = args.configurationParameter
def config = [:]
configParameters.each {
    def name = it.configurationParameterName
    def value = it.value
    config[name] = value
}

def credentials = args.credentials
assert credentials.size() == 1
def userName = credentials[0].userName
def password = credentials[0].password


//TODO get cluster topology

def endpoint = 'https://35.192.37.98'
def token = 'eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJkZWZhdWx0Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9zZWNyZXQubmFtZSI6InBvbGluYS10ZXN0LXRva2VuLWI1a2xxIiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9zZXJ2aWNlLWFjY291bnQubmFtZSI6InBvbGluYS10ZXN0Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9zZXJ2aWNlLWFjY291bnQudWlkIjoiNDY3YzU4MTYtMGE2OC0xMWU4LTkzY2QtNDIwMTBhODAwMThhIiwic3ViIjoic3lzdGVtOnNlcnZpY2VhY2NvdW50OmRlZmF1bHQ6cG9saW5hLXRlc3QifQ.6o76C3YiWpMhebTZf3f8pzFp4_-AJgzP4ySbFzGq3-00wzWQk1eWJyBxrcP_QaROwKfmA-ttC-oyOhUptDAdqyN4tnegPMppE8Ehx0gdrWXzx47EbKECXcI4P4HRh0O6IbAcYIyieoILcHSJQqs2bhczTJnw5MjbuJ8CcWyOAVOjRD3Ex7qiNFm-_27viX_qesJm-rD4EPumCEb8N3v7k5oDqlQTiY5dZemJ-FP8WrpuhklkwdvF44S-AUmFx5CcLfNt2gJ9ifAXkdl7ruOSrLqY7sdpjNBp1YzCY_iK1tPuTkGlLsq4_zt1R1UcgHDiK0VwkEVwiveH2xedZZUmzg'

import com.electriccloud.kubernetes.Client
def client = new Client(endpoint, token)
def namespaces = client.getNamespaces()
[namespaces: namespaces]
//println namespaces
//
//
//
//    {
//          nodes: [
//                {
//                  id: 'cluster1',
//                  type: 'cluster'
//                },
//                {
//                      id: 'default',
//                  type: 'namespace'
//                },        
//                {
//                      id: 'default::service1',
//                  type: 'service',
//                  displayName: 'service1',
//                  efId: 'UUID of microservice in ElectricFlow if this service was deployed by EF',
//                  status: 'Running, Failed...'  
//                },
//                {
//                      id: 'default::service2',
//                  type: 'service', //No efId since this service is not deployed by EF
//                  displayName: 'service2',
//                  status: 'Running, Failed...'  
//                },
//                {
//                      id: 'default::pod1',
//                  displayName: 'pod1',
//                  type: 'pod',
//                  status: 'Running, Failed...' 
//                },
//                {
//                      id: 'default::pod1::container1',
//                  type: 'container',
//                  displayName: 'ecdocker/motorbike:1.2',
//                  status: 'E.g., Running, Failed...',
//                  image: 'ecdocker/motorbike',
//                  version: '1.2'
//                }
//              ],
//      links: [
//                {
//                  source: 'cluster1',
//                  target: 'default'
//                },
//                {
//                      source: 'default',
//                  target: 'default::service1'
//                },
//                {
//                      source: 'default',
//                  target: 'default::service2'
//                },
//               {
//                      source: 'default::service1',
//                  target: 'default::pod1'
//               },
//               {
//                      source: 'default::pod1',
//                  target: 'default::pod1::container1'
//               }]
//    }