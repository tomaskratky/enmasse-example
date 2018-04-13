prerequisites:
```
git clone https://github.com/EnMasseProject/enmasse.git
cd enmasse/systemtests/
mvn clean package
mvn install:install-file -Dfile=target/systemtests-0.18-SNAPSHOT.jar -DgroupId=io.enmasse -DartifactId=enmasse -Dversion=0.18-SNAPSHOT -Dpackaging=jar
```
