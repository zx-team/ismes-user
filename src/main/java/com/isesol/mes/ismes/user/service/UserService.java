package com.isesol.mes.ismes.user.service;

import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.isesol.ismes.platform.core.service.bean.Dataset;
import com.isesol.ismes.platform.module.Bundle;
import com.isesol.ismes.platform.module.Parameters;
import com.isesol.ismes.platform.module.Sys;

public class UserService {

	public void getUserDepartments(Parameters parameters, Bundle bundle){
		String ryid = parameters.getString("ryid");
		if(StringUtils.isBlank(ryid)){
			return;
		}
//		Dataset dataSet = Sys.query("user", "bmid", " ryid = ? ", null, new Object[]{ryid});
//		if(dataSet == null){
//			return;
//		}
//		Map<String,Object> map = dataSet.getMap();
//		if(map == null || map.isEmpty()){
//			return;
//		}
//		bundle.put("bmid", map.get("bmid"));
		bundle.put("bmid", "10");
	}
	
	public void getUserInfoByYggh(Parameters parameters,Bundle bundle){
		String param = " 1=1 ";
		if(null!=parameters.getString("yggh")&&!"".equals(parameters.getString("yggh"))){
			param+=" and yggh = '"+parameters.getString("yggh")+"'";
		}
		Dataset dataset_ryxx = Sys.query("WH_RYJBXX","zzjgid,ryztdm,csrq,lxdh,rgz,xb,sfzh,xm,ryid,zhmm,yggh,bz,gwid,barcode,bzid,email,gzid,gs", param, null, new Object[]{});
		bundle.put("ryxx",  dataset_ryxx.getList());
	}
}
