package com.paynetSystem.paynetSystemBank.role.services;

import com.paynetSystem.paynetSystemBank.res.Response;
import com.paynetSystem.paynetSystemBank.role.entity.Role;

import java.util.List;


public interface RoleService {

        Response<Role> createRole(Role roleRequest);

        Response<Role> updateRole(Role roleRequest);

        Response<List<Role>> getAllRoles();

        Response<?> deleteRole(Long id);

}
