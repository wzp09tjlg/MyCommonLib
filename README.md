# MyCommonLib
just test upload aar to github

### 上传剥离的aar到github上，供日常开发中使用这些基础库。

#### 目标：
  1.抽离日常使用的基础模块
  2.统一管理这些基础模块的库
  3.提升开发效率
  
#### 一：如何上传上传本地的aar到github上进行托管。

  1.前提具备github账号，能够使用常用的git命令或者git管理工具
  
  2.创建剥离的公共的库，以android.library的形式。
```java
apply plugin: 'com.android.library'

...

//////// 打包发布 ////////
apply plugin: 'maven'

ext {
    // 从Github上clone下来的项目的本地地址
    GITHUB_REPO_PATH = "..."
    // api 'com.wuzp:common_lib:1.0.2'
    PUBLISH_GROUP_ID = 'com.wuzp'
    PUBLISH_ARTIFACT_ID = 'common_lib'
    PUBLISH_VERSION = '1.0.2'
}

uploadArchives {
    repositories.mavenDeployer {
        def deployPath = file(project.GITHUB_REPO_PATH)
        repository(url: "file://${deployPath.absolutePath}")
        pom.project {
            groupId project.PUBLISH_GROUP_ID
            artifactId project.PUBLISH_ARTIFACT_ID
            version project.PUBLISH_VERSION
        }
    }
}

// 源代码一起打包
task androidSourcesJar(type: Jar) {
    classifier = 'sources'
    from android.sourceSets.main.java.sourceFiles
}
artifacts {
    archives androidSourcesJar
}
```
  3.在项目的根目录下执行 
```java
   ./gradlew uploadArchives
```
  命令，会生成相应的描述文件以及aar文件。
  
  4.将本地修改的记录提交到本地仓库，并推送到github上的仓库中。
  
  5.在其他项目希望引入aar，在项目的build.gradle文件中引入
 ```java
  repositories {
       ...
       // GithubName 为你github的名字 ， repoName 为github上的仓库名。其他为模版不变 
        maven { url "https://raw.githubusercontent.com/GithubName/repoName/master"
    }
 ```
