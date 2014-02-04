/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package jchat.rest;

import jchat.data.dto.AuthenticationResultDto;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

@Path("/auth")
public class AuthenticationRest {

    @POST
    @Produces("application/json")
    public AuthenticationResultDto postUser(@FormParam("user") String user, @Context HttpServletRequest request) {
        AuthenticationResultDto dto = new AuthenticationResultDto();
        dto.setSessionId(request.getSession().getId());
        try {
            request.login(user, "");
            request.getSession().setAttribute("authenticated", Boolean.TRUE);
            dto.setSuccess(true);

        } catch (ServletException e) {
            dto.setSuccess(false);
            dto.setInfo("bad.username.or.password");
            request.getSession().setAttribute("authenticated", Boolean.FALSE);
        }
        return dto;
    }

    @GET
    public Response ping() {
        return Response.ok().build();
    }
}
