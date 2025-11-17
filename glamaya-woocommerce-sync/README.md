# glamaya-woocommerce-sync

./mvnw clean package -DskipTests && \
mkdir -p target/extracted && \
java -Djarmode=tools -jar target/*.jar extract --layers --launcher --destination target/extracted

docker buildx build \                                                                
--platform linux/amd64,linux/arm64 \
-t sonirahul/glamaya-woocommerce-sync:latest \
--push \
.