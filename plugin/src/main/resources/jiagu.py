#!/usr/bin/env python
# -*- coding: utf-8 -*-

import commands
import sys
import argparse
from os import path

reload(sys)
sys.setdefaultencoding('utf-8')

parser = argparse.ArgumentParser()
parser.add_argument('-apk_path', action='store', dest='apk_path', help='the apk path')
parser.add_argument('-jiagu_path', action='store', dest='jiagu_path', help='the jiagu path')
parser.add_argument('-username', action='store', dest='username', help='your 360 username')
parser.add_argument('-password', action='store', dest='password', help='your 360 password')
parser.add_argument('-key_password', action='store', dest='key_password', help='the password of keystore')
parser.add_argument('-key_store', action='store', dest='key_store', help='the keystore')
parser.add_argument('-key_alias', action='store', dest='key_alias', help='the keystore alias')
parser.add_argument('-key_alias_password', action='store', dest='key_alias_password', help='the keystore alias password')

argResult = parser.parse_args()
apk_path = argResult.apk_path
jiagu_path = argResult.jiagu_path
username = argResult.username
password = argResult.password
key_password = argResult.key_password
key_store = argResult.key_store
key_alias = argResult.key_alias
key_alias_password = argResult.key_alias_password

apk_dir = "app/build/outputs/apk/"
if apk_path and len(apk_path) > 0:
    apk_dir = path.dirname(apk_path)

login = "java -jar {jiaguPath}/jiagu.jar -login {username} {password}".format(jiaguPath=jiagu_path, username=username, password=password)
print login
importsign = "java -jar {jiaguPath}/jiagu.jar -importsign {key_store} {key_password} {key_alias} {key_alias_password}".format(jiaguPath=jiagu_path, key_store=key_store, key_password=key_password, key_alias=key_alias, key_alias_password=key_alias_password)
print importsign
jiagu = "java -jar {jiaguPath}/jiagu.jar -jiagu {apk_path} {apk_dir} -autosign".format(jiaguPath=jiagu_path, apk_path=apk_path, apk_dir=apk_dir)
print jiagu
result = commands.getoutput(login)
print result
result = commands.getoutput(importsign)
print result
result = commands.getoutput(jiagu)
print result