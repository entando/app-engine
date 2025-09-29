/*
 * Copyright 2021-Present Entando S.r.l. (http://www.entando.com) All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */
package org.entando.entando.plugins.jpmail.ent.system.services.model;

import jakarta.validation.constraints.NotBlank;
import java.util.Objects;

public class SMTPServerConfigurationDto {
    @NotBlank(message = "error.smtpServerConfig.host.notBlank")
    private String host;
    private Integer port;
    private Boolean checkServerIdentity;
    private Integer timeout;
    private String username;
    private String password;
    private String protocol;
    private Boolean active;
    private Boolean debugMode;

    public SMTPServerConfigurationDto() {
    }

    public Boolean isActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Boolean isDebugMode() {
        return debugMode;
    }

    public void setDebugMode(Boolean debugMode) {
        this.debugMode = debugMode;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public Boolean isCheckServerIdentity() {
        return checkServerIdentity;
    }

    public void setCheckServerIdentity(Boolean checkServerIdentity) {
        this.checkServerIdentity = checkServerIdentity;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SMTPServerConfigurationDto)) return false;
        SMTPServerConfigurationDto that = (SMTPServerConfigurationDto) o;
        return Objects.equals(isActive(), that.isActive()) &&
                Objects.equals(isDebugMode(), that.isDebugMode()) &&
                Objects.equals(getHost(), that.getHost()) &&
                Objects.equals(getPort(), that.getPort()) &&
                Objects.equals(getProtocol(), that.getProtocol()) &&
                Objects.equals(isCheckServerIdentity(), that.isCheckServerIdentity()) &&
                Objects.equals(getTimeout(), that.getTimeout()) &&
                Objects.equals(getUsername(), that.getUsername()) &&
                Objects.equals(getPassword(), that.getPassword());
    }

    @Override
    public int hashCode() {
        return Objects.hash(isActive(), isDebugMode(), getHost(), getPort(), isCheckServerIdentity(), getTimeout(), getUsername(), getPassword());
    }

    @Override
    public String toString() {
        return "SMTPServerConfigRequest{" +
                "active=" + active +
                ", debugMode=" + debugMode +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", protocol='" + protocol +'\'' +
                ", checkServerIdentity=" + checkServerIdentity +
                ", timeout=" + timeout +
                ", username='" + username + '\'' +
                '}';
    }
}
