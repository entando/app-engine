package org.entando.entando.keycloak.services.oidc.model;

import com.agiletec.aps.system.services.user.User;

// Classes added for "Casa Tecnologie Emergenti" project
public class KeycloakUser extends User {
    
    private UserRepresentation userRepresentation;

    @Override
	public boolean isEntandoUser() {
		return false;
	}
    
    @Override
	@Deprecated
    public boolean isJapsUser() {
        return this.isEntandoUser();
    }
    
    @Override
    public Object clone() {
        KeycloakUser cl = new KeycloakUser();
        cl.setUsername(this.getUsername());
        cl.setPassword("");
        cl.setAuthorizations(this.getAuthorizations());
        cl.setUserRepresentation(this.getUserRepresentation());
        return cl;
    }

    public UserRepresentation getUserRepresentation() {
        return userRepresentation;
    }
    public void setUserRepresentation(UserRepresentation userRepresentation) {
        this.userRepresentation = userRepresentation;
    }
    
}
