import os, yaml
CUR_FPATH = os.path.abspath(__file__)
CUR_FDIR = os.path.dirname(CUR_FPATH)
# chdir to the directory of this script
os.chdir(CUR_FDIR)


import os
def os_system_sure(command):
    print(f"执行命令：{command}")
    result = os.system(command)
    if result != 0:
        print(f"命令执行失败：{command}")
        exit(1)
    print(f"命令执行成功：{command}\n\n")


# https://github.com/openfaas/faas-netes/tree/master/chart/openfaas
os_system_sure("git clone https://github.com/apache/openwhisk-deploy-kube.git")
os.chdir("openwhisk-deploy-kube")
os_system_sure("git checkout 146d24925564e2871f4cc6506d4e92098968457d")