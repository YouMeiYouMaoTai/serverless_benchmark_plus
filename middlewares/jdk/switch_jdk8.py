
### chdir
import os
CUR_FPATH = os.path.abspath(__file__)
CUR_FDIR = os.path.dirname(CUR_FPATH)
# chdir to the directory of this script
os.chdir(CUR_FDIR)


### utils
def os_system_sure(command):
    print(f"执行命令：{command}")
    result = os.system(command)
    if result != 0:
        print(f"命令执行失败：{command}")
        exit(1)
    print(f"命令执行成功：{command}")



bins=[
        "java",
        "javac",
        "jcmd",
    ]
OPENJDK="/usr/lib/jvm/java-8-openjdk-amd64/"

# check exist
if not os.path.exists(OPENJDK):
    print("!!! openjdk 8 not found")
    exit(1)

# swicth back to openjdk
for bin in bins:
    os_system_sure(f"update-alternatives --install /usr/bin/{bin} {bin} {OPENJDK}bin/{bin} 100")
    os_system_sure(f"update-alternatives --set {bin} {OPENJDK}bin/{bin}")