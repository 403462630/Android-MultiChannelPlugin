#!/usr/bin/env python
# -*- coding: utf-8 -*-
import zipfile
import json
import urllib2
import commands
import sys
from os import path
import argparse

reload(sys)
sys.setdefaultencoding('utf-8')

def getJsonData(str):
    try:
        fjson = file(str)
        jsonobj = json.load(fjson)
        fjson.close
    except IOError:
        jsonobj = json.load(urllib2.urlopen(str))
    return jsonobj

try:
    H = dict(line.strip().split('=') for line in open('local.properties') if not line.startswith('#') and not line.startswith('\n'))
except IOError:
    H = dict(line.strip().split('=') for line in open('../local.properties') if not line.startswith('#') and not line.startswith('\n'))
sdk = H['sdk.dir']

apk_signer_str = commands.getoutput("find {sdk}/build-tools/ -name 'apksigner'".format(sdk=sdk))
apk_signer_arr = apk_signer_str.split('\n')
print apk_signer_arr
apk_signer = apk_signer_arr[-1]

key_password = '123456'
base_dir = "app/build/outputs/apk"
base_apk = "app-debug.apk"
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
argResult = parser.parse_args()
apk_path = argResult.apk_path
json_path = argResult.json_path
key_password = argResult.key_password
key_store = argResult.key_store
default_apk_name = argResult.default_apk_name
channel_ids = argResult.channel_ids
ex_channel_ids = argResult.ex_channel_ids

if apk_path and len(apk_path) > 0:
    base_dir = path.dirname(apk_path)
    base_apk = path.basename(apk_path)
    print("base_dir: {base_dir}, base_apk: {base_apk}".format(base_dir=base_dir, base_apk=base_apk))
if channel_ids and len(channel_ids) > 0:
    channel_ids = channel_ids.split(',')
if ex_channel_ids and len(ex_channel_ids) > 0:
    ex_channel_ids = ex_channel_ids.split(',')

if not key_store:
    key_store_str = commands.getoutput("find . -maxdepth 1 -name '*.keystore' ")
    key_store_arr = key_store_str.split('\n')
    print key_store_arr
    if key_store_arr[0] == '':
        key_store_str = commands.getoutput("find . -maxdepth 1 -name '*.jks' ")
        key_store_arr = key_store_str.split('\n')
    key_store = key_store_arr[0]

data = getJsonData(json_path)

commands.getoutput("rm -rf {base}/META-INF".format(base=base_dir))
commands.getoutput("mkdir {base}/META-INF".format(base=base_dir))
commands.getoutput("find {base} -type f -not -name '{base_apk}' -delete".format(base=base_dir, base_apk=base_apk))
for channel in data:
    if channel_ids and not (str(channel["id"]) in channel_ids):
        continue
    if ex_channel_ids and (str(channel["id"]) in ex_channel_ids):
        continue
    empty_channel_file_name = "fc-multi-channel-{channel}-{channel_id}".format(channel=channel["code"], channel_id=channel["id"])
    empty_channel_file = "{base}/META-INF/{empty_channel_file_name}".format(base=base_dir, empty_channel_file_name=empty_channel_file_name)
    commands.getoutput("touch {empty_channel_file}".format(empty_channel_file=empty_channel_file))
    if channel.has_key("extInfo"):
        channel_file_writer = open(empty_channel_file, 'w')
        channel_file_writer.write(json.dumps(channel["extInfo"], ensure_ascii=False))
        channel_file_writer.close()
    apk_name = ("{base}/" + default_apk_name).format(base=base_dir, code=channel['code'], id=channel['id'], name=channel['name'])
    commands.getoutput("cp {base}/{base_apk} {new_apk}".format(base=base_dir, new_apk=apk_name, base_apk=base_apk))
    zipped = zipfile.ZipFile(apk_name, 'a', zipfile.ZIP_DEFLATED)
    zipped.write(empty_channel_file, "META-INF/{empty_channel_file_name}".format(channel=channel["code"], empty_channel_file_name=empty_channel_file_name))
    zipped.close()
    if apk_signer == '':
        print 'apksigner {apk_name}: warning no found apksigner command'.format(apk_name=apk_name)
    elif key_store == '':
        print 'apksigner {apk_name}: warning no found keystore'.format(apk_name=apk_name)
    else:
        signapk = "echo '{key_password}' | {apk_signer} sign --ks {key_store} {apk_name}".format(key_password=key_password, apk_signer=apk_signer, key_store=key_store, apk_name=apk_name)
        print signapk
        result = commands.getoutput(signapk)
        result_array = result.split('\n')
        if len(result_array) > 1:
            print result
        else:
            print 'apksigner {apk_name} success'.format(apk_name=apk_name)