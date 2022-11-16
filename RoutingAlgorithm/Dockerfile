FROM openjdk
ENV MYPATH /home/algorithm
WORKDIR $MYPATH
# 将本目录下target的RoutingAlgorithm-1.0-SNAPSHOT.jar复制到容器/home/algorithm目录下
ADD ./target/RoutingAlgorithm-1.0-SNAPSHOT.jar ./RoutingAlgorithm.jar
EXPOSE 1053
# 容器启动运行jar包
ENTRYPOINT ["java", "-jar", "./RoutingAlgorithm.jar"]