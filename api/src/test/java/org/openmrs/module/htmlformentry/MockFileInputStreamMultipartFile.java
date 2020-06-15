package org.openmrs.module.htmlformentry;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * A mock implementation of MultipartFile that ensures that we deal with a FileInputStream under the
 * hood.
 */
public class MockFileInputStreamMultipartFile implements MultipartFile {
	
	private final String name;
	
	private String originalFilename;
	
	private String contentType;
	
	private byte[] bytes;
	
	private File file;
	
	public MockFileInputStreamMultipartFile(String name, String originalFilename, String contentType, File file) {
		Assert.hasLength(name, "Name must not be null");
		this.name = name;
		this.originalFilename = (originalFilename != null ? originalFilename : "");
		this.contentType = contentType;
		this.file = file;
	}
	
	@Override
	public String getName() {
		return this.name;
	}
	
	@Override
	public String getOriginalFilename() {
		return this.originalFilename;
	}
	
	@Override
	public String getContentType() {
		return this.contentType;
	}
	
	@Override
	public boolean isEmpty() {
		try {
			return (getBytes().length == 0);
		}
		catch (IOException e) {
			e.printStackTrace();
			assert (false);
		}
		return true;
	}
	
	@Override
	public long getSize() {
		try {
			return getBytes().length;
		}
		catch (IOException e) {
			e.printStackTrace();
			assert (false);
		}
		return 0;
	}
	
	@Override
	public byte[] getBytes() throws IOException {
		if (bytes == null) {
			bytes = IOUtils.toByteArray(getInputStream());
		}
		return bytes;
	}
	
	@Override
	public InputStream getInputStream() throws IOException {
		// that's the key thing, pass the FileInputStream as late as possible in the
		// process
		return new FileInputStream(file);
	}
	
	@Override
	public void transferTo(File dest) throws IOException, IllegalStateException {
		FileCopyUtils.copy(getBytes(), dest);
	}
	
}
