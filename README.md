## katbox

![Maven Central](https://img.shields.io/maven-central/v/net.ormr.katbox/katbox?label=release&style=for-the-badge)

katbox is a Kotlin multiplatform wrapper written with [ktor](https://ktor.io/docs/client.html) and coroutines, for 
interacting with the permanent file hosting service [Catbox](https://catbox.moe), and its temporary file hosting service
equivalent [Litterbox](https://litterbox.catbox.moe/). All public endpoints defined by
[Catbox](https://catbox.moe/tools.php) and [Litterbox](https://litterbox.catbox.moe/tools.php) are implemented.

## Installation

```kotlin
repositories { 
    mavenCentral()
}

dependencies {
    implementation("net.ormr.katbox:katbox:${RELEASE_VERSION}")
}
```

**katbox does not define any ktor-client engine by itself, therefore you will need to define one yourself. Information 
can be found [here](https://ktor.io/docs/http-client-engines.html).**

## Usage

katbox separates any anonymous operations from logged in operations by placing all anonymous operations in the `Catbox` 
companion object. Therefore any functions defined there do not require a userhash.

All functions are documented properly, so only the very basics will be shown here, for further information, reading the 
documentation on the functions.

### Uploading files

All `upload` functions return a `String`, which contains the url to the uploaded file. 

**Anonymously**
```kotlin
// upload raw bytes
Catbox.upload(byteArray(4, 2), "foo.bar")
// upload from an url
Catbox.upload(Url("http://i.imgur.com/aksF5Gk.jpg"))
// if on the JVM, upload via Path
Catbox.upload(Path("./foo/bar.foobar"))

// litterbox only allows anonymous uploads
// upload raw bytes
Litterbox.upload(byteArray(4, 2), "foo.bar")
// if on the JVM, upload via Path
Litterbox.upload(Path("./foo/bar.foobar"))
```

**As user**
```kotlin
// to upload as a user, a Catbox instance must be created
// no verification on whether the userHash is valid is done, so be careful
val catbox = Catbox(userHash = "####")
// upload raw bytes
catbox.upload(byteArray(4, 2), "foo.bar")
// upload from an url
catbox.upload(Url("http://i.imgur.com/aksF5Gk.jpg"))
// if on the JVM, upload via Path
catbox.upload(Path("./foo/bar.foobar"))
```

## Creating an album with newly uploaded images

This example is a bit contrived, but at least serves as somewhat of a real-world example.

```kotlin
val catbox = Catbox(userHash = "####")
val files = Path("./foo/").listDirectoryEntries(glob = "*.png").map { catbox.upload(it) }
val myCoolAlbum = catbox.createAlbum(
    title = "My Cool Images", 
    description = "A collection of all my cool images.",
    files = files.mapTo(hashSet()) { it.substringAfterLast('/') },
)
```