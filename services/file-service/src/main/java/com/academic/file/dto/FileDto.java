package com.academic.file.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileDto {
	private String fileId;
	private String fileType;
	private String fileName;
	private long size;
	private String uploader;
	private String url;
	private String uploadTime;
	private String updateTime;
	private String permission;
}
