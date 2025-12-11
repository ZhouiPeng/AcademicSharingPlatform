package com.academic.file.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileCheckDto {
	private String fileId;
	private String fileName;
	private long size;
	private List<String> owners;
	private String uploader;
	private String uploadTime;
	private String updateTime;
	private String permission;
}
