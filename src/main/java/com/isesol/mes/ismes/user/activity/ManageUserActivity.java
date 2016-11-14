package com.isesol.mes.ismes.user.activity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.isesol.ismes.platform.core.service.bean.Dataset;
import com.isesol.ismes.platform.core.service.bean.Role;
import com.isesol.ismes.platform.module.Bundle;
import com.isesol.ismes.platform.module.Parameters;
import com.isesol.ismes.platform.module.Sys;

public class ManageUserActivity {
	
	private Logger log = LoggerFactory.getLogger(this.getClass());
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
	private BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
	
	/**
	 * 保存用户角色
	 * @param parameters
	 * @param bundle
	 * @return
	 */
	public String saveRoles(Parameters parameters, Bundle bundle) {
		String gh = parameters.getString(Constants.USER_ID);
		Object[] roles = parameters.getValues("roles[]");
		List<Map<String, Object>> userRoleMappings = Lists.newArrayList();
		for (int i = 0; i < roles.length; i++) {
			String[] roleModule = String.valueOf(roles[i]).split(":");
			String role = roleModule[0];
			String module = roleModule[1];
			Map<String, Object> mapping = Maps.newHashMap();
			mapping.put(Constants.ROLE_NAME, role);
			mapping.put(Constants.MODULE_NAME, module);
			mapping.put(Constants.USER_YGGH, gh);
			userRoleMappings.add(mapping);
		}
		this.addUserRoleMappings(userRoleMappings);
		return "json:";
	}
	
	public String deleteRoles(Parameters parameters, Bundle bundle) {
		String yggh = parameters.getString(Constants.USER_ID);
		Object[] roles = parameters.getValues("roles[]");
		for (int i = 0; i < roles.length; i++) {
			String[] roleModule = String.valueOf(roles[i]).split(":");
			String role = roleModule[0];
			String module = roleModule[1];
			Sys.delete(Constants.USER_ROLE_MODEL_NAME,
					Constants.USER_YGGH + "=? and " + Constants.ROLE_NAME + "=? and " + Constants.MODULE_NAME + "=?", yggh,
					role, module);
		}
		return "json:";
	}
	
	public String listCj(Parameters parameters, Bundle bundle) {
		Parameters p = new Parameters();
		List<Map<String, Object>> cjData = (List<Map<String, Object>>)((Bundle) Sys.callModuleService("org", "cjService", p)).get("data");
		List<Map<String, Object>> cj = Lists.newArrayList();
		for (Map<String, Object> map : cjData) {
			Map<String, Object> cjElement = Maps.newHashMap();
			cjElement.put("label", map.get("zzjgmc"));
			cjElement.put("value", map.get("zzjgid"));
			cj.add(cjElement);
		}
		bundle.put("cj", cj);
		return "json:cj";
	}
	
	public String listGw(Parameters parameters, Bundle bundle) {
		// 车间ID
		String cjId = parameters.getString("parent");
		Dataset dataset = Sys.query("user_gwxxb", "gwid,gwmc", "zzjgid=?", null, null, cjId);
		List<Map<String, Object>> gw = Lists.newArrayList();
		for (Map<String, Object> map : dataset.getList()) {
			Map<String, Object> cjElement = Maps.newHashMap();
			cjElement.put("label", map.get("gwmc"));
			cjElement.put("value", map.get("gwid"));
			gw.add(cjElement);
		}
		bundle.put("gw", gw);
		return "json:gw";
	}
	
	/**
	 * 获取用户列表
	 * @param parameters
	 * @param bundle
	 * @return
	 */
	public String listUser(Parameters parameters, Bundle bundle) {
//		Sys.callModuleService("org", "orgService", parameters);
		return "listUser";
	}
	
	/**
	 * 用户列表数据
	 * @param parameters
	 * @param bundle
	 * @return
	 */
	public String userTable(Parameters parameters, Bundle bundle) {
		int page = Integer.parseInt(parameters.get("page").toString());
		int pageSize = Integer.parseInt(parameters.get("pageSize").toString());
		Dataset dataset = Sys.query(Constants.USER_MODEL_NAME, getAllUserFields(), null, Constants.USER_RYID + " asc",
				(page - 1) * pageSize, pageSize, null);
		bundle.put("rows", changeIdToName(dataset.getList()));
		int totalPage = dataset.getTotal() % pageSize == 0 ? dataset.getTotal() / pageSize
				: dataset.getTotal() / pageSize + 1;
		bundle.put("totalPage", totalPage);
		bundle.put("currentPage", page);
		bundle.put("totalRecord", dataset.getTotal());
		return "json:";
	}
	
	private List<Map<String, Object>> changeIdToName(List<Map<String, Object>> list) {
		for (Map<String, Object> map : list) {
			map.put("zzjgid", getOrgName(String.valueOf(map.get("zzjgid"))));
			map.put("gwid", getGwName(String.valueOf(map.get("gwid"))));
		}
		return Lists.newArrayList(list);
	}
	
	private String getOrgName(String id) {
		if ("".equals(id) || "null".equals(id)) {
			return "";
		}
		Parameters p = new Parameters();
		p.set("id", id);
		Bundle bundle = Sys.callModuleService("org", "nameService", p);
		if (bundle == null) {
			return "";
		}
		return String.valueOf(bundle.get("name"));
	}
	
	private String getGwName(String id) {
		if ("".equals(id) || "null".equals(id)) {
			return "";
		}
		Dataset dataset = Sys.query("user_gwxxb", "gwmc", "gwid=?", null, null, id);
		if (dataset.getCount() == 0) {
			return "";
		}
		return String.valueOf(dataset.getList().get(0).get("gwmc"));
	}
	
	/**
	 * 添加用户
	 * @param parameters
	 * @param bundle
	 * @return
	 */
	public String addUser(Parameters parameters, Bundle bundle) {
		// 用于标识是否是通过提交表单进入的方法
		Object submitFlag = parameters.get("submitFlag");
		if (submitFlag == null) {
			// 不是通过表单提交,进入用户添加页面
			return "addUser";
		}
		
		Map<String, Object> formData = null;
		try {
			formData = getUserFormData(parameters, "add");
		} catch (ParseException e) {
			log.error(e.getMessage());
			bundle.setError( "日期格式错误");
			return "addUser";
		}
		
		// 查询是否已存在本次提交的工号
		Dataset dataset = Sys.query(Constants.USER_MODEL_NAME, Constants.USER_YGGH, Constants.USER_YGGH + "='" +formData.get(Constants.USER_YGGH)+ "'", null);
		if (dataset.getCount() >= 1) {
			bundle.setError( "重复的工号提交");
			bundle.put("user", formData);
			return "addUser";
		}
		
		Sys.insert(Constants.USER_MODEL_NAME, formData);
		
		// 返回用户列表页面
		if ("2".equals(submitFlag)) {
			return "redirect:addUser";
		} else {
			return "redirect:listUser";
		}
	}
	
	/**
	 * 修改用户
	 * @param parameters
	 * @param bundle
	 * @return
	 */
	public String modifyUser(Parameters parameters, Bundle bundle) {
		// 用于标识是否是通过提交表单进入的方法
		Object submitFlag = parameters.get("submitFlag");
		if (submitFlag == null) {
			// 不是通过表单提交,进入用户修改页面并初始化用户信息
			Map<String, Object> user = getUser(this.getParameter(parameters, Constants.USER_RYID));
			if (user == null) {
				bundle.setError( "错误的用户记录数");
			} else {
				// 放入原始的工号,用来识别用户修改动作是否修改了工号
				user.put("old_gh", user.get(Constants.USER_YGGH));
				try {
					if (user.get(Constants.USER_CSRQ) != null && !"".equals(user.get(Constants.USER_CSRQ))) {
						user.put(Constants.USER_CSRQ, sdf.parse(String.valueOf(user.get(Constants.USER_CSRQ))));
					}
				} catch (ParseException e) {
					e.printStackTrace();
				}
				bundle.put("user", user);
			}
			return "modifyUser";
		}
		// 处理表单提交
		Map<String, Object> formData = null;
		try {
			formData = getUserFormData(parameters, "modify");
		} catch (ParseException e) {
			log.error(e.getMessage());
			bundle.setError( "日期格式错误");
			return "modifyUser";
		}
		
		String oldGh = getParameter(parameters, "old_gh");
		if (!oldGh.equals(formData.get(Constants.USER_YGGH))) {
			// 如果表单提交修改了工号, 查询新工号是否与数据库中现有工号重复
			Dataset dataset = Sys.query(Constants.USER_MODEL_NAME, Constants.USER_YGGH, Constants.USER_YGGH + "='" +formData.get(Constants.USER_YGGH)+ "'", null);
			if (dataset.getCount() >= 1) {
				bundle.setError( "重复的工号提交");
				formData.put("old_gh", oldGh);
				bundle.put("user", formData);
				return "modifyUser";
			}
		}
		// 获取并移除人员代码(主键)
		String rydm = String.valueOf(formData.remove(Constants.USER_RYID));
		Sys.update(Constants.USER_MODEL_NAME, formData, Constants.USER_RYID + "=?", Integer.parseInt(rydm));
		return "redirect:listUser";
	}
	
	/**
	 * 删除用户
	 * @param parameters
	 * @param bundle
	 * @return
	 */
	public String deleteUser(Parameters parameters, Bundle bundle) {
		String rydm = this.getParameter(parameters, Constants.USER_RYID);
		Sys.delete(Constants.USER_MODEL_NAME, Constants.USER_RYID + "=?", Integer.parseInt(rydm));
		return "redirect:listUser";
	}
	
	/**
	 * 修改用户密码
	 * @param parameters
	 * @param bundle
	 * @return
	 */
	public String modifyPassword(Parameters parameters, Bundle bundle) {
		// 不是通过表单提交,进入用户修改页面并初始化用户信息
		Map<String, Object> user = getUser(this.getParameter(parameters, Constants.USER_RYID));
		bundle.put("user", user);
		return "modifyPassword";
	}
	
	public String doModifyPassword(Parameters parameters, Bundle bundle) {
		String rydm = this.getParameter(parameters, Constants.USER_RYID);
		String zhmm = this.getParameter(parameters, Constants.USER_ZHMM);
		String oldPassword = getParameter(parameters, "old_password");
		String password = this.getParameter(parameters, "password");
		String confirmPassword = this.getParameter(parameters, "confirm_password");
		if (!password.equals(confirmPassword)) {
			bundle.put("error", "两次输入的新密码不一致");
			return "json:";
		}
		if (!encoder.matches(oldPassword, zhmm)) {
			bundle.setError( "输入的旧密码错误");
			bundle.put("error", "输入的旧密码错误");
			return "json:";
		}
		// 修改用户密码
		Map<String, Object> user = Maps.newHashMap();
		user.put(Constants.USER_ZHMM, encoder.encode(password));
		Sys.update(Constants.USER_MODEL_NAME, user, Constants.USER_RYID + "=?", Integer.parseInt(rydm));
		return "json:";
	}
	
	/**
	 * 用户角色分配页
	 * @param parameters
	 * @param bundle
	 * @return
	 */
	public String listUserRole(Parameters parameters, Bundle bundle) {
		return "listUserRole";
	}
	
	/**
	 * 已选角色列表数据
	 * @param parameters
	 * @param bundle
	 * @return
	 */
	public String selectedRolesTable(Parameters parameters, Bundle bundle) {
		getRoleTable(parameters, bundle, true);
		return "json:";
	}
	
	/**
	 * 备选角色列表数据
	 * @param parameters
	 * @param bundle
	 * @return
	 */
	public String availableRolesTable(Parameters parameters, Bundle bundle) {
		getRoleTable(parameters, bundle, false);
		return "json:";
	}
	
	private void getRoleTable(Parameters parameters, Bundle bundle, boolean selectedRoles) {
		String userId = parameters.getString(Constants.USER_ID);
		if (userId == null) {
			return;
		}
		// 查询该用户对应的角色
		String[] models = new String[]{Constants.USER_MODEL_NAME, Constants.USER_ROLE_MODEL_NAME};
		String join = Constants.USER_MODEL_NAME + " join " + Constants.USER_ROLE_MODEL_NAME + " on " + Constants.USER_MODEL_NAME + "." + Constants.USER_YGGH + "=" + Constants.USER_ROLE_MODEL_NAME + "." + Constants.USER_YGGH;
		String fields = Constants.ROLE_NAME + "," + Constants.USER_ROLE_YHJSID + "," + Constants.MODULE_NAME;
		String conditions = Constants.USER_MODEL_NAME + "." + Constants.USER_ID + "=?";
		Dataset dataset = Sys.query(models, join, fields, conditions, null, null, userId);
		Map<String, Boolean> temp = Maps.newHashMap();
		List<Map<String, Object>> rows = dataset.getList();
		for (Map<String, Object> row : rows) {
			// 以模块名称和角色名称为key来标识当前用户角色
			temp.put(String.valueOf(row.get(Constants.MODULE_NAME)) + "," + String.valueOf(row.get(Constants.ROLE_NAME)), true);
		}
		List<Map<String, Object>> result = Lists.newArrayList();
		List<Role> roles = Sys.getRoles();
		for (Role role : roles) {
			Map<String, Object> map = Maps.newHashMap();
			map.put("label", role.getLabel());
			map.put("desc", role.getDescription());
			map.put("module", role.getModule());
			map.put("name", role.getName() + ":" + role.getModule());
			if (selectedRoles) {
				if (temp.get(role.getModule() + "," + role.getName()) != null) {
					result.add(map);
				}
			} else {
				// available roles
				if (temp.get(role.getModule() + "," + role.getName()) == null) {
					result.add(map);
				}
			}
		}
		
		int page = Integer.parseInt(parameters.get("page").toString());
		int pageSize = Integer.parseInt(parameters.get("pageSize").toString());
		int totalPage = result.size() % pageSize == 0 ? result.size() / pageSize
				: result.size() / pageSize + 1;
		
		bundle.put("totalPage", totalPage);
		bundle.put("currentPage", page);
		bundle.put("totalRecord", result.size());
		bundle.put("rows", getPageData(result, page, pageSize, totalPage));
	}
	
	private List<Map<String, Object>> getPageData(List<Map<String, Object>> data, int page, int pageSize,
			int totalPage) {
		List<Map<String, Object>> result = Lists.newArrayList();
		if (data.size() <= pageSize) {
			return data;
		}
		for (int i = (page - 1) * pageSize; i < data.size(); i++) {
			if (result.size() == pageSize) {
				break;
			}
			result.add(data.get(i));
		}
		return result;
	}
	
	/**
	 * 获取用户提交的表单数据
	 * @param parameters
	 * @param operation
	 * @return
	 * @throws ParseException
	 */
	private Map<String, Object> getUserFormData(Parameters parameters, String operation) throws ParseException {
		Map<String, Object> data = Maps.newHashMap();
		data.put(Constants.USER_YGGH, getParameter(parameters, Constants.USER_YGGH));
		data.put(Constants.USER_XM, getParameter(parameters, Constants.USER_XM));
		data.put(Constants.USER_XB, getParameter(parameters, Constants.USER_XB));
		data.put(Constants.USER_CSRQ, sdf.parse(getParameter(parameters, Constants.USER_CSRQ)));
		data.put(Constants.USER_SFZH, getParameter(parameters, Constants.USER_SFZH));
		data.put(Constants.USER_LXDH, getParameter(parameters, Constants.USER_LXDH));
		data.put(Constants.USER_EMAIL, getParameter(parameters, Constants.USER_EMAIL));
		data.put(Constants.USER_GWID, getParameter(parameters, Constants.USER_GWID));
		data.put(Constants.USER_ZZJGID, getParameter(parameters, Constants.USER_ZZJGID));
//		data.put(Constants.USER_GZID, getParameter(parameters, Constants.USER_GZID));
//		data.put(Constants.USER_BZID, getParameter(parameters, Constants.USER_BZID));
		data.put(Constants.USER_RGZ, getParameter(parameters, Constants.USER_RGZ));
		data.put(Constants.USER_BZ, getParameter(parameters, Constants.USER_BZ));
		data.put(Constants.USER_RYZT_DM, getParameter(parameters, Constants.USER_RYZT_DM));
		data.put(Constants.USER_BARCODE, getParameter(parameters, Constants.USER_BARCODE));
		if (operation.equals("add")) {
			// 默认用户密码为身份证号后4位
			String zhmm = encoder.encode(getParameter(parameters, Constants.USER_SFZH).substring(getParameter(parameters, Constants.USER_SFZH).length() - 4));
			data.put(Constants.USER_ZHMM, zhmm);
		} else if (operation.equals("modify")) {
			data.put(Constants.USER_RYID, getParameter(parameters, Constants.USER_RYID));
		}
		return data;
	}
	
	/**
	 * 获取parameter
	 * @param parameters
	 * @param name
	 * @return
	 */
	private String getParameter(Parameters parameters, String name) {
		if (parameters.get(name) == null || "".equals(parameters.get(name))) {
			if (Constants.USER_GWID.equals(name) || Constants.USER_RGZ.equals(name)) {
				return "0";
			}
			if (Constants.USER_XB.equals(name)) {
				return "10";
			}
			return "";
		}
		return (String)parameters.get(name);
	}
	
	/**
	 * 根据人员代码获取用户数据
	 * @param rydm
	 * @return
	 */
	private Map<String, Object> getUser(String rydm) {
		// 构造查询fields
		
		// 查询用户信息
		Dataset dataset = Sys.query(Constants.USER_MODEL_NAME, getAllUserFields(), Constants.USER_RYID + "=?", null, null, rydm);
		if (dataset.getCount() != 1) {
			return null;
		}
		return dataset.getList().get(0);
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
		fields.append(Constants.USER_ZZJGID);
		fields.append(",");
		fields.append(Constants.USER_GWID);
		fields.append(",");
		fields.append(Constants.USER_RGZ);
		fields.append(",");
		fields.append(Constants.USER_BARCODE);
		fields.append(",");
		fields.append(Constants.USER_BZ);
		fields.append(",");
		fields.append(Constants.USER_RYZT_DM);
		return fields.toString();
	}
	
	/**
	 * 增加用户和角色绑定
	 * @param rydm 人员代码
	 * @param roleName 角色名称
	 * @param module 模块名称
	 */
	private void addUserRoleMappings(List<Map<String, Object>> userRoleMappings) {
		Sys.insert(Constants.USER_ROLE_MODEL_NAME, userRoleMappings);
	}

}
