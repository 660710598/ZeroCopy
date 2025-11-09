fsutil file createnew TestFileSent 524288000
fsutil file createnew TestFileSent 1073741824

cd C:\Users\realr\OneDrive\เอกสาร\ZeroCopy1\src

javac *.java

java ZeroCopyServer

java ZeroCopyClient

java BenchmarkCopy


java ZeroCopyServer_ThreadPool
java ZeroCopyClient_Thread

java NormalCopyServer

java NormalCopyClient
