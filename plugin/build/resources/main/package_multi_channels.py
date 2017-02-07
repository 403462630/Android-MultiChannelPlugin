import zipfile
import json
import urllib2
import commands
import sys
from os import path

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
apk_signer = apk_signer_arr[0]

key_password = '123456'
base_dir = "app/build/outputs/apk"
base_apk = "app-debug.apk"
json_path = ''
key_store = ''
channel_ids = []
default_apk_name = 'app-{code}.apk'

argv_len = len(sys.argv)
if argv_len >= 2:
    apk_path = sys.argv[1]
    base_dir = path.dirname(apk_path)
    base_apk = path.basename(apk_path)
    print("base_dir: {base_dir}, base_apk: {base_apk}".format(base_dir=base_dir, base_apk=base_apk))
if argv_len >= 3:
    json_path = sys.argv[2]
if argv_len >= 4:
    key_password = sys.argv[3]
if argv_len >= 5:
    key_store = sys.argv[4]
if argv_len >= 6:
    default_apk_name = sys.argv[5]
if argv_len >= 7:
    channel_ids = sys.argv[6].split(',')
if key_store == '':
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
    empty_channel_file_name = "pbchannel_{channel}_{channel_id}".format(channel=channel["code"], channel_id=channel["id"])
    empty_channel_file = "{base}/META-INF/{empty_channel_file_name}".format(base=base_dir, empty_channel_file_name=empty_channel_file_name)
    commands.getoutput("touch {empty_channel_file}".format(empty_channel_file=empty_channel_file))
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
      result = commands.getoutput(signapk)
      result_array = result.split('\n')
      if len(result_array) > 1:
          print result
      else:
          print 'apksigner {apk_name} success'.format(apk_name=apk_name)