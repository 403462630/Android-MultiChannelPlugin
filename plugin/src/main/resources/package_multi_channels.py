#!/usr/bin/env python
# -*- coding: utf-8 -*-
import zipfile
import json
import os, sys
from os import path
import argparse
if sys.version_info.major <= 2:
  import urllib2
  import commands as subprocess
  reload(sys)
  sys.setdefaultencoding("utf-8")
else:
  import urllib.request as urllib2
  import subprocess

def getJsonData(str):
    try:
        fjson = open(str, "r")
        jsonobj = json.load(fjson)
        fjson.close
    except IOError:
        jsonobj = json.load(urllib2.urlopen(str))
    return jsonobj

# try:
#     H = dict(line.strip().split('=') for line in open('local.properties') if not line.startswith('#') and not line.startswith('\n'))
# except IOError:
#     H = dict(line.strip().split('=') for line in open('../local.properties') if not line.startswith('#') and not line.startswith('\n'))
sdk = '/Users/fangcan/Documents/Android/sdk' #H['sdk.dir']

apk_signer_str = subprocess.getoutput("find {sdk}/build-tools/ -name 'apksigner'".format(sdk=sdk))
apk_signer_arr = apk_signer_str.split('\n')
print(apk_signer_arr)
apk_signer = apk_signer_arr[-1]

key_password = '123456'
base_dir = "app/build/outputs/apk"
base_apk = "app-debug.apk"
jiagu_base_apk = ''
json_path = ''
key_store = ''
channel_ids = []
default_apk_name = 'app-{code}.apk'


parser = argparse.ArgumentParser()
parser.add_argument('-apk_path', action='store', dest='apk_path', help='the apk path')
parser.add_argument('-json_path', action='store', dest='json_path', help='the channel data config path')
parser.add_argument('-key_password', action='store', dest='key_password', help='the password of keystore')
parser.add_argument('-key_store', action='store', dest='key_store', help='the keystore')
parser.add_argument('-default_apk_name', action='store', dest='default_apk_name', help='the default apk name')
parser.add_argument('-channel_ids', action='store', dest='channel_ids', help='the channel packaged')
parser.add_argument('-ex_channel_ids', action='store', dest='ex_channel_ids', help='the channel to exclude')
parser.add_argument('-jiagu_channel_ids', action='store', dest='jiagu_channel_ids', help='the channel packaged')
parser.add_argument('-jiagu_ex_channel_ids', action='store', dest='jiagu_ex_channel_ids', help='the channel to exclude')
parser.add_argument('-is_enable_jiagu', action='store', dest='is_enable_jiagu', help='the is_enable_jiagu to exclude')


argResult = parser.parse_args()
apk_path = argResult.apk_path
json_path = argResult.json_path
key_password = argResult.key_password
key_store = argResult.key_store
default_apk_name = argResult.default_apk_name
jiagu_default_apk_name = "jiagu_" + default_apk_name

channel_ids = argResult.channel_ids
ex_channel_ids = argResult.ex_channel_ids

is_enable_jiagu = argResult.is_enable_jiagu
jiagu_channel_ids = argResult.jiagu_channel_ids
jiagu_ex_channel_ids = argResult.jiagu_ex_channel_ids

if apk_path and len(apk_path) > 0:
    base_dir = path.dirname(apk_path)
    base_apk = path.basename(apk_path)
    print("base_dir: {base_dir}, base_apk: {base_apk}".format(base_dir=base_dir, base_apk=base_apk))

jiagu_apk_path = ''
if is_enable_jiagu == 'true':
    jiagu_apk_path = subprocess.getoutput("find {base_dir} -name '*jiagu_sign.apk'".format(base_dir=base_dir))
    if jiagu_apk_path and len(jiagu_apk_path) > 0:
        jiagu_base_apk = path.basename(jiagu_apk_path)
        print("jiagu_base_apk: {jiagu_base_apk}".format(jiagu_base_apk=jiagu_base_apk))

if channel_ids and len(channel_ids) > 0:
    channel_ids = channel_ids.split(',')
if ex_channel_ids and len(ex_channel_ids) > 0:
    ex_channel_ids = ex_channel_ids.split(',')

if jiagu_channel_ids and len(jiagu_channel_ids) > 0:
    jiagu_channel_ids = jiagu_channel_ids.split(',')
if jiagu_ex_channel_ids and len(jiagu_ex_channel_ids) > 0:
    jiagu_ex_channel_ids = jiagu_ex_channel_ids.split(',')

if not key_store:
    key_store_str = subprocess.getoutput("find . -maxdepth 1 -name '*.keystore' ")
    key_store_arr = key_store_str.split('\n')
    print(key_store_arr)
    if key_store_arr[0] == '':
        key_store_str = subprocess.getoutput("find . -maxdepth 1 -name '*.jks' ")
        key_store_arr = key_store_str.split('\n')
    key_store = key_store_arr[0]

data = getJsonData(json_path)

subprocess.getoutput("rm -rf {base}/META-INF".format(base=base_dir))
subprocess.getoutput("mkdir {base}/META-INF".format(base=base_dir))


def packagingApk(p_channels, p_ex_channel_ids, p_base_apk, p_default_apk_name):
    for channel in data:
        if p_channels and not (str(channel["id"]) in p_channels):
            continue
        if p_ex_channel_ids and (str(channel["id"]) in p_ex_channel_ids):
            continue
        empty_channel_file_name = "fc-multi-channel-{channel}-{channel_id}".format(channel=channel["code"], channel_id=channel["id"])
        empty_channel_file = "{base}/META-INF/{empty_channel_file_name}".format(base=base_dir, empty_channel_file_name=empty_channel_file_name)
        subprocess.getoutput("touch {empty_channel_file}".format(empty_channel_file=empty_channel_file))
        if "extInfo" in channel:
            channel_file_writer = open(empty_channel_file, 'w')
            channel_file_writer.write(json.dumps(channel["extInfo"], ensure_ascii=False))
            channel_file_writer.close()
        apk_name = ("{base}/" + p_default_apk_name).format(base=base_dir, code=channel['code'], id=channel['id'], name=channel['name'])
        subprocess.getoutput("cp {base}/{base_apk} {new_apk}".format(base=base_dir, new_apk=apk_name, base_apk=p_base_apk))
        zipped = zipfile.ZipFile(apk_name, 'a', zipfile.ZIP_DEFLATED)
        zipped.write(empty_channel_file, "META-INF/{empty_channel_file_name}".format(channel=channel["code"], empty_channel_file_name=empty_channel_file_name))
        zipped.close()
        if apk_signer == '':
            print('apksigner {apk_name}: warning no found apksigner command'.format(apk_name=apk_name))
        elif key_store == '':
            print('apksigner {apk_name}: warning no found keystore'.format(apk_name=apk_name))
        else:
            signapk = "echo '{key_password}' | {apk_signer} sign --ks {key_store} {apk_name}".format(key_password=key_password, apk_signer=apk_signer, key_store=key_store, apk_name=apk_name)

            result = subprocess.getoutput(signapk)
            result_array = result.split('\n')
            if len(result_array) > 1:
                print(result)
            else:
                print('apksigner {apk_name} success'.format(apk_name=apk_name))

if jiagu_base_apk and len(jiagu_base_apk) > 0:
    subprocess.getoutput("find {base} -type f -not -name '{base_apk}' -not -name '{jiagu_base_apk}' -name '*.apk' -delete".format(base=base_dir, jiagu_base_apk=jiagu_base_apk, base_apk=base_apk))
    packagingApk(channel_ids, ex_channel_ids, base_apk, default_apk_name)
    packagingApk(jiagu_channel_ids, jiagu_ex_channel_ids, jiagu_base_apk, jiagu_default_apk_name)
else:
    subprocess.getoutput("find {base} -type f -not -name '{base_apk}' -name '*.apk' -delete".format(base=base_dir, base_apk=base_apk))
    packagingApk(channel_ids, ex_channel_ids, base_apk, default_apk_name)


