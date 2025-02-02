package org.entando.entando.aps.system.services.userprofile;

import com.agiletec.aps.system.services.user.UserDetails;
import org.entando.entando.aps.system.services.userprofile.model.AvatarDto;
import org.entando.entando.ent.exception.EntException;
import org.entando.entando.web.userprofile.model.ProfileAvatarRequest;
import org.springframework.validation.BindingResult;

public interface IAvatarService {
    
    AvatarDto getAvatarData(UserDetails userDetails);

    String updateAvatar(ProfileAvatarRequest request, UserDetails userDetails,
            BindingResult bindingResult);

    void deleteAvatar(UserDetails userDetails, BindingResult bindingResult);
    
    void deleteAvatar(String username) throws EntException;
    
}
