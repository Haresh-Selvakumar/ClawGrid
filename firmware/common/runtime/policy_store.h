#pragma once
#include "policy_record.h"
#include "nvs.h"
#include "nvs_flash.h"

class PolicyStore {
public:
    bool begin(uint32_t &boot,PolicyConfig &config) {
        if(nvs_flash_init()!=ESP_OK || nvs_open("clawgrid_a",NVS_READWRITE,&handle_)!=ESP_OK) return false;
        uint32_t previous=0; esp_err_t e=nvs_get_u32(handle_,"boot",&previous);
        bool fresh=e==ESP_ERR_NVS_NOT_FOUND;
        if((e!=ESP_OK && !fresh) || previous==UINT32_MAX) return false;
        uint8_t bytes[kPolicyRecordBytes]; size_t size=sizeof(bytes);
        e=nvs_get_blob(handle_,"policy",bytes,&size);
        if(e==ESP_ERR_NVS_NOT_FOUND && fresh) {
            encode_policy(config,bytes);
            if(nvs_set_blob(handle_,"policy",bytes,sizeof(bytes))!=ESP_OK) return false;
        } else if(e!=ESP_OK || !decode_policy(bytes,size,config)) return false;
        uint8_t manual[kManualRecordBytes]; size=sizeof(manual);
        e=nvs_get_blob(handle_,"manual",manual,&size);
        if(e==ESP_ERR_NVS_NOT_FOUND) config.manual_flags=0;
        else if(e!=ESP_OK || !decode_manual_policy(manual,size,config.manual_flags)) return false;
        uint8_t sensor[kSensorPolicyBytes]; size=sizeof(sensor);
        e=nvs_get_blob(handle_,"sensor_rules",sensor,&size);
        if(e!=ESP_ERR_NVS_NOT_FOUND && (e!=ESP_OK || !decode_sensor_policy(sensor,size,config))) return false;
        uint8_t order[kSensorOrderBytes]; size=sizeof(order);
        e=nvs_get_blob(handle_,"sensor_order",order,&size);
        if(e!=ESP_ERR_NVS_NOT_FOUND && (e!=ESP_OK || !decode_sensor_order(order,size,config.temperature_first))) return false;
        // Commit identity before permitting any radio command. Never erase NVS
        // to recover an error, and never silently wrap a boot identity.
        boot=previous+1;
        if(nvs_set_u32(handle_,"boot",boot)!=ESP_OK || nvs_commit(handle_)!=ESP_OK) { boot=0; return false; }
        return true;
    }
    bool save(const PolicyConfig &config) {
        if(!handle_ || !valid_policy(config)) return false;
        uint8_t bytes[kPolicyRecordBytes]; encode_policy(config,bytes);
        uint8_t manual[kManualRecordBytes]; encode_manual_policy(config.manual_flags,manual);
        uint8_t sensor[kSensorPolicyBytes]; encode_sensor_policy(config,sensor);
        uint8_t order[kSensorOrderBytes]; encode_sensor_order(config.temperature_first,order);
        return nvs_set_blob(handle_,"sensor_order",order,sizeof(order))==ESP_OK && nvs_set_blob(handle_,"sensor_rules",sensor,sizeof(sensor))==ESP_OK && nvs_set_blob(handle_,"policy",bytes,sizeof(bytes))==ESP_OK &&
            nvs_set_blob(handle_,"manual",manual,sizeof(manual))==ESP_OK && nvs_commit(handle_)==ESP_OK;
    }
private:
    nvs_handle_t handle_=0;
};
