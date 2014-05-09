/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.badlogic.gdx.tools.imagepacker;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.regex.Pattern;

import com.badlogic.gdx.tools.FileProcessor;
import com.badlogic.gdx.tools.imagepacker.TexturePacker2.Settings;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.ObjectMap;

/** @author Nathan Sweet */
public class TexturePackerFileProcessor extends FileProcessor {
	private final Settings defaultSettings;
	private ObjectMap<File, Settings> dirToSettings = new ObjectMap();
	private Json json = new Json();
	private String packFileName;
	private File root;

	public TexturePackerFileProcessor () {
		this(new Settings(), "pack.atlas");
	}

	public TexturePackerFileProcessor (Settings defaultSettings, String packFileName) {
		this.defaultSettings = defaultSettings;

		if (packFileName.indexOf('.') == -1) packFileName += ".atlas";
		this.packFileName = packFileName;

		setFlattenOutput(true);
		addInputSuffix(".png", ".jpg");
	}

	public ArrayList<Entry> process (File inputFile, File outputRoot) throws Exception {
		root = inputFile;
		return super.process(inputFile, outputRoot);
	}

	public ArrayList<Entry> process (File[] files, File outputRoot) throws Exception {
		// Delete pack file and images.
		if (outputRoot.exists()) {
			new File(outputRoot, packFileName).delete();
			FileProcessor deleteProcessor = new FileProcessor() {
				protected void processFile (Entry inputFile) throws Exception {
					inputFile.inputFile.delete();
				}
			};
			deleteProcessor.setRecursive(false);

			String prefix = packFileName;
			int dotIndex = prefix.lastIndexOf('.');
			if (dotIndex != -1) prefix = prefix.substring(0, dotIndex);
			deleteProcessor.addInputRegex(Pattern.quote(prefix) + "\\d*\\.(png|jpg)");

			deleteProcessor.process(outputRoot, null);
		}
		return super.process(files, outputRoot);
	}

	protected void processDir (Entry inputDir, ArrayList<Entry> files) throws Exception {
		System.out.println(inputDir.inputFile.getName());

		// Start with a copy of a parent dir's settings or the default settings.
		Settings settings = null;
		File parent = inputDir.inputFile;

		String outputImageFileName = inputDir.inputFile.getName();
		if (parent.equals(root)) {
			outputImageFileName = packFileName;
		}

		while (true) {
			if (parent.equals(root)) break;
			parent = parent.getParentFile();
			settings = dirToSettings.get(parent);
			if (settings != null) {
				settings = new Settings(settings);
				break;
			}
		}
		if (settings == null) settings = new Settings(defaultSettings);
		dirToSettings.put(inputDir.inputFile, settings);

		// Merge settings from pack.json file.
		File settingsFile = new File(inputDir.inputFile, "pack.json");
		if (settingsFile.exists()) json.readFields(settings, new JsonReader().parse(new FileReader(settingsFile)));

		// Pack.
		TexturePacker2 packer = new TexturePacker2(root, settings);
		for (Entry file : files)
			packer.addImage(file.inputFile);
		if(settings.useImageNameAsInnerFolderName) {
			packer.pack(inputDir.outputDir, packFileName, outputImageFileName);
		} else {
			packer.pack(inputDir.outputDir, packFileName, inputDir.inputFile.getName());
		}
	}
}
