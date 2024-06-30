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
os_system_sure("helm repo add openwhisk https://openwhisk.apache.org/charts")
os_system_sure("helm repo update")
# os_system_sure("helm install owdev openwhisk/openwhisk -n openwhisk --create-namespace -f custom_ow.yaml")
os_system_sure("helm install owdev ./openwhisk-deploy-kube/helm/openwhisk -n openwhisk --create-namespace -f custom_ow.yaml")