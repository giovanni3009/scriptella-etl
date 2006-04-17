/*
 * Copyright 2006 The Scriptella Project Team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package scriptella.configuration;

/**
 * TODO: Add documentation
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class ConnectionEl extends XMLConfigurableBase {
    public static final String DEFAULT_ID = "\"DEFAULT\"";
    private String id;
    private String url;
    private String driver;
    private String user;
    private String password;
    private String catalog;
    private String schema;

    public ConnectionEl() {
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }

    public String getDriver() {
        return driver;
    }

    public void setDriver(final String driver) {
        this.driver = driver;
    }

    public String getUser() {
        return user;
    }

    public void setUser(final String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public String getCatalog() {
        return catalog;
    }

    public void setCatalog(final String catalog) {
        this.catalog = catalog;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(final String schema) {
        this.schema = schema;
    }

    public void configure(final XMLElement element) {
        setProperty(element, "id");
        setProperty(element, "url");
        setProperty(element, "driver");
        setProperty(element, "user");
        setProperty(element, "password");
        setProperty(element, "catalog");
        setProperty(element, "schema");

        if (id == null) {
            id = DEFAULT_ID;
        }
    }

    public String toString() {
        return "ConnectionEl{" + "id='" + id + "'" + ", url='" + url + "'" +
                ", driver='" + driver + "'" + ", user='" + user + "'" + ", password='" +
                password + "'" + ", catalog='" + catalog + "'" + ", schema='" + schema +
                "'" + "}";
    }
}