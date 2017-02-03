filePath=$(dirname $0)
cd $filePath/..

if [ $# -eq 0 ]; then
 DEPLOY=0
elif [ "$1" == "--deploy" ]; then
 DEPLOY=1 
else
 DEPLOY=0
fi

rm ./out/EC-Kubernetes.jar
rm ./EC-Kubernetes.zip

jar cvf ./out/EC-Kubernetes.jar dsl/ META-INF/ pages/ lib/ htdocs/
zip -r ./EC-Kubernetes.zip dsl/ META-INF/ pages/ lib/ htdocs/

if [ $DEPLOY -eq 1 ]; then
  echo "Installing plugin ..."
  ectool --server localhost login admin changeme
  ectool installPlugin ./out/EC-Kubernetes.jar --force 1
  ectool promotePlugin EC-Kubernetes-1.0.0.11
fi  