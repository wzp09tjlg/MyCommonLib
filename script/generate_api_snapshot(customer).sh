#!/bin/sh
folder=$(cd "$(dirname "$0")";cd ../../soda-applications/customer-aar/src;pwd)"/qa/assets/apiSnapshot"
host="http://dev.dida.me:8097"

token="token=p1TXDOnEzn-_Aqqpu3fxpz0aesPJ5jLvIlAXOTm0uNBUjb0OAkEIhF_FTE0BmXC38jbnv9WZ3RiLje8ulFJA8vFlZmJDAIITwo9GMpc7qb4ILghTmuCKmAe8tjE-ey_qRl20ratg7O9-TkG_gtv_657ZZlpDbS1rHkXyPsv_BQAA__8="
deviceId="deviceId=a079c234e6ff3845582291fdd128a8de"
networkType="networkType=wifi"
requestId="requestId=1"
clientType="clientType=2"
versionCode="versionCode=307"
deviceType="deviceType=zty"
timestamp="timestamp=1519733002"
suuid="suuid=4777AADE51E16E85F2E7E081FA65E8AA_0"
deviceBrand="deviceBrand=Android"
lat="lat=40.07179"
lng="lng=116.24322"
poiLat="poiLat=31.48791"
poiLng="poiLng=120.2998"
appVersion="appVersion=5.1.31"
osType="osType=2"
osVersion="osVersion=6.0"
channel="channel=0"
cityId="cityId=1"
bizId="bizId=379"
poiId="poiId=11013758097395894265"
#公共参数
commonParam=${token}"&"${deviceId}"&"${networkType}"&"${requestId}"&"${clientType}"&"${versionCode}"&"${deviceType}"&"${timestamp}"&"${suuid}"&"${deviceBrand}"&"${lat}"&"${lng}"&"${poiLat}"&"${poiLng}"&"${appVersion}"&"${osType}"&"${osVersion}"&"${channel}"&"${cityId}"&"${bizId}"&"${poiId}

commonParam="orderBy=0&lastShopId=0&page=0&count=20&moduleRecId=&shopRecId=&deviceId=c767213a849d4a26c03c383c4b746d02&wifiMac=BC%3A3D%3A85%3A67%3AEC%3A8A&locationType=1&versionCode=307&token=7z4b23IXqxCTOk8KQkernDfbU_E2EtEEdOrpTbXy_ZRMxzuuwkAMheGtXP21C499J4zPbniER4GQGFFF2Tst5bdxRGCcUIz_nhGZi9eh0riglsaKtj_m6_M-r8iN-Zyo9YioZXjfjeuvjRuijapwz-yJcUc4xgO1_RsAAP__&poiName=%E5%8D%8E%E5%BA%9C%E5%A4%A9%E5%9C%B0%C2%B7%E5%B0%8A%E5%9B%AD&timestamp=1522296882&suuid=29CB8FFC16EA26C4AA912797CE588F16_0&deviceBrand=HUAWEI&wifiName=%22YDWX-Wireless%22&poiLat=31.48791&poiLng=120.2998&linuxKernel=4.1.18-g86ace34&appVersion=5.1.36&imei=867540038707045B5CEA9830CC86E0276B413F371898A87&osVersion=7.0&channel=0&cityId=1&operatorName=&extActivityId=&poiId=2000000000027126906&networkType=wifi&clientType=2&enterChannel=&deviceType=RNE-AL00&mapType=%E6%9C%AA%E7%9F%A5&lng=116.23355376796992&poiCityId=47&osType=2&ip=172.21.197.179&lat=40.06784773410955&bizId=379"

mkdir ${folder}

curl -o ${folder}/feed_index.json -d ${commonParam}"&orderBy=0&lastShopId=''&page=0&count=15" ${host}"/feed/index"

shopIdArray=(1152921581430505570 1152921702444564569 1152921541177770089 1152921608500543577
1152921520449519710 1152921634295513183 1152921634828189787 1152921541920161891
1152921651722846312 1152921721813860442 1152921540343103580 1152921752105123941
1152921514325835875 1152921740893749341 1152921505459077221)

for shopId in ${shopIdArray[@]}
do
    curl -o ${folder}/shop_index_${shopId}.json -d ${commonParam}"&shopId="${shopId} ${host}"/shop/index"
done

for shopId in ${shopIdArray[@]}
do
    curl -o ${folder}/shop_detail_${shopId}.json -d ${commonParam}"&shopId="${shopId} ${host}"/shop/detail"
done

goodsIdArray=(1152921505953416180 1152921514019062733 1152921619497419823 1152921522344756307)

for goodsId in ${goodsIdArray[@]}
do
    curl -o ${folder}/item_detail_${goodsId}.json -d ${commonParam}"&itemId="${goodsId}"&needShopInfo=1" ${host}"/item/detail"
done

curl -o ${folder}/order_unfinished.json -d ${commonParam}"" ${host}"/order/unfinished"

curl -o ${folder}/address_list.json -d ${commonParam}"" ${host}"/address/list"

curl -o ${folder}/address_recommend.json -d ${commonParam}"" ${host}"/address/recommend"

####
###
# 这个文件是在以后学习shell脚本的时候来看的，shell脚本 是非常方便脚本文件，只需要 构建就能获得简单的数据
#
#
####