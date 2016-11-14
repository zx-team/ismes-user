package com.isesol.mes.ismes.user.activity;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.isesol.ismes.platform.core.service.bean.Dataset;
import com.isesol.ismes.platform.module.Bundle;
import com.isesol.ismes.platform.module.Parameters;
import com.isesol.ismes.platform.module.Sys;

public class SecurityActivity {
	
	public void getUserInfo(Parameters parameters, Bundle bundle) {
		Dataset dataset = null;
		if(parameters.getString("account")==null){
			String token = parameters.getString("token");
			dataset = Sys.query(Constants.USER_MODEL_NAME, getAllUserFields(), Constants.USER_BARCODE + "=?", null, null, token);
		}else{
			String account = parameters.getString("account");
			dataset = Sys.query(Constants.USER_MODEL_NAME, getAllUserFields(), Constants.ACCOUNT + "=?", null, null, account);
		}
		
		if (dataset.getCount() != 1) {
			return;
		}
		bundle.put("user", dataset.getList().get(0));
	}
	
	public void getUserInfoByID(Parameters parameters, Bundle bundle) {
		String userId = parameters.getString("id");
		Dataset dataset = Sys.query(Constants.USER_MODEL_NAME, getAllUserFields(), Constants.USER_ID + "=?", null, null, userId);
		if (dataset.getCount() != 1) {
			return;
		}
		bundle.put("user", dataset.getList().get(0));
	}
	
	/**
	 * 获取用户Model所有字段
	 * @return
	 */
	private String getAllUserFields() {
		StringBuffer fields = new StringBuffer();
		fields.append(Constants.USER_RYID);
		fields.append(",");
		fields.append(Constants.USER_ZHMM);
		fields.append(",");
		fields.append(Constants.USER_YGGH);
		fields.append(",");
		fields.append(Constants.USER_XM);
		fields.append(",");
		fields.append(Constants.USER_XB);
		fields.append(",");
		fields.append(Constants.USER_CSRQ);
		fields.append(",");
		fields.append(Constants.USER_SFZH);
		fields.append(",");
		fields.append(Constants.USER_LXDH);
		fields.append(",");
		fields.append(Constants.USER_EMAIL);
		fields.append(",");
		fields.append(Constants.USER_GWID);
		fields.append(",");
		fields.append(Constants.USER_ZZJGID);
		fields.append(",");
		fields.append(Constants.USER_GZID);
		fields.append(",");
		fields.append(Constants.USER_BZID);
		fields.append(",");
		fields.append(Constants.USER_RGZ);
		fields.append(",");
		fields.append(Constants.USER_BZ);
		fields.append(",");
		fields.append(Constants.USER_RYZT_DM);
		fields.append(",");
		fields.append(Constants.USER_BARCODE);
		return fields.toString();
	}	
	/**
	 * 根据用户ID获取相关角色信息
	 * @param parameters
	 * @param
	 */
	public void getRolesByUserId(Parameters parameters, Bundle bundle) {
		String userId = parameters.getString("userId");
		// 查询该用户对应的角色
		Dataset dataset = Sys.query(Constants.USER_ROLE_MODEL_NAME, Constants.MODULE_NAME +","+ Constants.ROLE_NAME, Constants.USER_ID + "=?", null, null, userId);
		if (dataset.getCount() == 0) {
			return;
		}
		List<String> moduleRoles = Lists.newArrayList();
		for (Map<String, Object> row : dataset.getList()) {
			// moduleName,roleName
			moduleRoles.add(String.valueOf(row.get(Constants.MODULE_NAME)) + "," + String.valueOf(row.get(Constants.ROLE_NAME)));
		}
		bundle.put("moduleRoles", moduleRoles);
	}
}
