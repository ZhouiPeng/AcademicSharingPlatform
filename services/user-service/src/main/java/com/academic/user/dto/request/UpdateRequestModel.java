package com.academic.user.dto.request;

public class UpdateRequestModel
{
    private String displayName;
    private String avatarFileId;
    private String email;

    public String getDisplayName()
    {
        return displayName;
    }

    public String getAvatarFileId()
    {
        return avatarFileId;
    }

    public String getEmail()
    {
        return email;
    }
}
