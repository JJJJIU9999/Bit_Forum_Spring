# 1、地基：拿一个装了JDK 17 的基础镜像
FROM eclipse-temurin:17-jdk

# 2、工作目录：容器里的所有操作都在/app 下进行
WORKDIR /app

# 3、搬东西：把本地打包好的JAR复制到容器里
COPY target/bit-forum-spring-0.0.1-SNAPSHOT.jar app.jar

# 4、开门：告诉外界容器会监听8080端口
EXPOSE 8080

# 5、启动：容器一运行就执行这个命令
ENTRYPOINT [ "java","-jar","app.jar" ]