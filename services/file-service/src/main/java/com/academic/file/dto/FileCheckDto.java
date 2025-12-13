package com.academic.file.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileCheckDto {
	private String fileId;
	private String fileName;
	private long size;
	private String url;
	private String uploader;
	private String uploadTime;
	private String updateTime;
	private String permission;
}
