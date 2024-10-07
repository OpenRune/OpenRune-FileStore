package net.runelite.cache.item;

import java.awt.image.BufferedImage;
import java.io.IOException;
import dev.openrune.cache.CacheManager;
import dev.openrune.game.item.ItemSpriteBuilder;
import net.runelite.cache.definitions.ItemDefinition;
import net.runelite.cache.definitions.ModelDefinition;
import net.runelite.cache.definitions.providers.ItemProvider;
import net.runelite.cache.definitions.providers.ModelProvider;
import net.runelite.cache.definitions.providers.SpriteProvider;
import net.runelite.cache.definitions.providers.TextureProvider;
import net.runelite.cache.models.FaceNormal;
import net.runelite.cache.models.JagexColor;
import net.runelite.cache.models.VertexNormal;

public class ItemSpriteFactory {

	public ItemProvider itemProvider;
	public ModelProvider modelProvider;
	public SpriteProvider spriteProvider;
	public TextureProvider textureProvider;

	public ItemSpriteFactory(ItemProvider itemProvider, ModelProvider modelProvider, SpriteProvider spriteProvider, TextureProvider textureProvider) {
		this.itemProvider = itemProvider;
		this.modelProvider = modelProvider;
		this.spriteProvider = spriteProvider;
		this.textureProvider = textureProvider;
	}

	public BufferedImage createSprite(ItemSpriteBuilder itemSpriteFactory) throws IOException {
		SpritePixels spritePixels = createSpritePixels(itemSpriteFactory);
		return spritePixels == null ? null : spritePixels.toBufferedImage();
	}

	private SpritePixels createSpritePixels(ItemSpriteBuilder itemSpriteFactory) throws IOException {
		int itemId = itemSpriteFactory.getItemID();
		int quantity = itemSpriteFactory.getQuantity();
		int border = itemSpriteFactory.getBorder();
		int shadowColor = itemSpriteFactory.getShadowColor();
		boolean noted = itemSpriteFactory.isNoted();

		ItemDefinition item = itemProvider.provide(itemId);

		if (quantity > 1 && item.countObj != null) {
			for (int i = 0; i < 10; ++i) {
				if (quantity >= item.countCo[i] && item.countCo[i] != 0) {
					item = itemProvider.provide(item.countObj[i]);
					break;
				}
			}
		}


		Model itemModel = getModel(modelProvider, item);
		if (itemModel == null) return null;

		SpritePixels auxSpritePixels = null;
		if (item.notedTemplate != -1) {
			auxSpritePixels = createSpritePixels(new ItemSpriteBuilder(item.notedID).copy(itemSpriteFactory).quantity(10).border(1).shadowColor(0).noted(true));
		} else if (item.boughtTemplateId != -1) {
			auxSpritePixels = createSpritePixels(new ItemSpriteBuilder(item.boughtId).copy(itemSpriteFactory).quantity(quantity).border(border).shadowColor(0).noted(false));
		} else if (item.placeholderTemplateId != -1) {
			auxSpritePixels = createSpritePixels(new ItemSpriteBuilder(item.placeholderId).copy(itemSpriteFactory).quantity(quantity).border(0).shadowColor(0).noted(false));
		}
		if (auxSpritePixels == null && (item.notedTemplate != -1 || item.boughtTemplateId != -1 || item.placeholderTemplateId != -1)) return null;

		RSTextureProvider rsTextureProvider = new RSTextureProvider(textureProvider, spriteProvider);
		rsTextureProvider.brightness = JagexColor.BRIGHTNESS_MAX;

		SpritePixels spritePixels = new SpritePixels(itemSpriteFactory.getWidth(), itemSpriteFactory.getHeight());
		Graphics3D graphics = new Graphics3D(rsTextureProvider);
		graphics.setBrightness(JagexColor.BRIGHTNESS_MAX);
		graphics.setRasterBuffer(spritePixels.pixels, itemSpriteFactory.getWidth(), itemSpriteFactory.getHeight());
		graphics.reset();
		graphics.setRasterClipping();
		graphics.setOffset(itemSpriteFactory.getWidth() / 2, itemSpriteFactory.getHeight() / 2);
		graphics.rasterGouraudLowRes = false;

		if (item.placeholderTemplateId != -1) auxSpritePixels.drawAtOn(graphics, 0, 0);

		int zoom2d = itemSpriteFactory.getZoom2d();
		if (noted) zoom2d = (int) (zoom2d * 1.5D);
		else if (border == 2) zoom2d = (int) (zoom2d * 1.04D);

		int var17 = zoom2d * Graphics3D.SINE[itemSpriteFactory.getXan2d()] >> 16;
		int var18 = zoom2d * Graphics3D.COSINE[itemSpriteFactory.getXan2d()] >> 16;

		itemModel.calculateBoundsCylinder();
		itemModel.projectAndDraw(graphics, 0, itemSpriteFactory.getYan2d(), itemSpriteFactory.getZan2d(), itemSpriteFactory.getXan2d(),
				itemSpriteFactory.getXOffset2d(), itemModel.modelHeight / 2 + var17 + itemSpriteFactory.getYOffset2d(), var18 + itemSpriteFactory.getYOffset2d());

		if (item.boughtTemplateId != -1) auxSpritePixels.drawAtOn(graphics, 0, 0);

		if (border >= 1) spritePixels.drawBorder(1);
		if (border >= 2) spritePixels.drawBorder(0xffffff);
		if (shadowColor != 0) spritePixels.drawShadow(shadowColor);

		graphics.setRasterBuffer(spritePixels.pixels, itemSpriteFactory.getWidth(), itemSpriteFactory.getHeight());
		if (item.notedTemplate != -1) auxSpritePixels.drawAtOn(graphics, 0, 0);

		graphics.setRasterBuffer(graphics.graphicsPixels, graphics.graphicsPixelsWidth, graphics.graphicsPixelsHeight);
		graphics.setRasterClipping();
		graphics.rasterGouraudLowRes = true;

		return spritePixels;
	}

	private static Model getModel(ModelProvider modelProvider, ItemDefinition item) throws IOException {
		ModelDefinition inventoryModel = modelProvider.provide(item.inventoryModel);
		if (inventoryModel == null) return null;

		if (item.resizeX != 128 || item.resizeY != 128 || item.resizeZ != 128) {
			inventoryModel.resize(item.resizeX, item.resizeY, item.resizeZ);
		}

		if (item.colorFind != null) {
			for (int i = 0; i < item.colorFind.length; i++) {
				inventoryModel.recolor(item.colorFind[i], item.colorReplace[i]);
			}
		}

		if (item.textureFind != null) {
			for (int i = 0; i < item.textureFind.length; i++) {
				inventoryModel.retexture(item.textureFind[i], item.textureReplace[i]);
			}
		}

		return light(inventoryModel, item.ambient + 64, item.contrast + 768, -50, -10, -50);
	}

	private static Model light(ModelDefinition def, int ambient, int contrast, int x, int y, int z) {
		def.computeNormals();
		int magnitude = (int) Math.sqrt(x * x + y * y + z * z);
		int adjustedContrast = magnitude * contrast >> 8;

		Model litModel = new Model();
		litModel.faceColors1 = new int[def.faceCount];
		litModel.faceColors2 = new int[def.faceCount];
		litModel.faceColors3 = new int[def.faceCount];

		if (def.numTextureFaces > 0 && def.textureCoords != null) {
			int[] textureCount = new int[def.numTextureFaces];
			for (int i = 0; i < def.faceCount; i++) {
				if (def.textureCoords[i] != -1) textureCount[def.textureCoords[i] & 255]++;
			}

			litModel.numTextureFaces = 0;
			for (int count : textureCount) if (count > 0) litModel.numTextureFaces++;

			litModel.texIndices1 = new int[litModel.numTextureFaces];
			litModel.texIndices2 = new int[litModel.numTextureFaces];
			litModel.texIndices3 = new int[litModel.numTextureFaces];

			int texIdx = 0;
			int[] textureMapping = new int[def.numTextureFaces];
			for (int i = 0; i < def.numTextureFaces; i++) {
				if (textureCount[i] > 0 && def.textureRenderTypes[i] == 0) {
					litModel.texIndices1[texIdx] = def.texIndices1[i] & 0xffff;
					litModel.texIndices2[texIdx] = def.texIndices2[i] & 0xffff;
					litModel.texIndices3[texIdx] = def.texIndices3[i] & 0xffff;
					textureMapping[i] = texIdx++;
				} else {
					textureMapping[i] = -1;
				}
			}

			litModel.textureCoords = new byte[def.faceCount];
			for (int i = 0; i < def.faceCount; i++) {
				litModel.textureCoords[i] = def.textureCoords[i] != -1 ? (byte) textureMapping[def.textureCoords[i] & 255] : -1;
			}
		}

		for (int i = 0; i < def.faceCount; i++) {
			byte faceType = def.faceRenderTypes != null ? def.faceRenderTypes[i] : 0;
			byte faceAlpha = def.faceTransparencies != null ? def.faceTransparencies[i] : 0;
			short faceTexture = def.faceTextures != null ? def.faceTextures[i] : -1;

			if (faceAlpha == -2) faceType = 3;
			if (faceAlpha == -1) faceType = 2;

			VertexNormal vertexNormal;
			int tmp;
			FaceNormal faceNormal;

			if (faceTexture == -1) {
				if (faceType != 0) {
					if (faceType == 1) {
						faceNormal = def.faceNormals[i];
						tmp = (y * faceNormal.y + z * faceNormal.z + x * faceNormal.x) / (adjustedContrast / 2 + adjustedContrast) + ambient;
						litModel.faceColors1[i] = method2608(def.faceColors[i] & 0xffff, tmp);
						litModel.faceColors3[i] = -1;
					} else if (faceType == 3) {
						litModel.faceColors1[i] = 128;
						litModel.faceColors3[i] = -1;
					} else {
						litModel.faceColors3[i] = -2;
					}
				} else {
					int faceColor = def.faceColors[i] & 0xffff;
					vertexNormal = def.vertexNormals[def.faceIndices1[i]];
					tmp = (y * vertexNormal.y + z * vertexNormal.z + x * vertexNormal.x) / (adjustedContrast * vertexNormal.magnitude) + ambient;
					litModel.faceColors1[i] = method2608(faceColor, tmp);
					vertexNormal = def.vertexNormals[def.faceIndices2[i]];
					tmp = (y * vertexNormal.y + z * vertexNormal.z + x * vertexNormal.x) / (adjustedContrast * vertexNormal.magnitude) + ambient;
					litModel.faceColors2[i] = method2608(faceColor, tmp);
					vertexNormal = def.vertexNormals[def.faceIndices3[i]];
					tmp = (y * vertexNormal.y + z * vertexNormal.z + x * vertexNormal.x) / (adjustedContrast * vertexNormal.magnitude) + ambient;
					litModel.faceColors3[i] = method2608(faceColor, tmp);
				}
			} else if (faceType == 1) {
				faceNormal = def.faceNormals[i];
				tmp = (y * faceNormal.y + z * faceNormal.z + x * faceNormal.x) / (adjustedContrast / 2 + adjustedContrast) + ambient;
				litModel.faceColors1[i] = bound2to126(tmp);
				litModel.faceColors3[i] = -1;
			} else {
				litModel.faceColors3[i] = -2;
			}
		}

		litModel.verticesCount = def.vertexCount;
		litModel.verticesX = def.vertexX;
		litModel.verticesY = def.vertexY;
		litModel.verticesZ = def.vertexZ;
		litModel.indicesCount = def.faceCount;
		litModel.indices1 = def.faceIndices1;
		litModel.indices2 = def.faceIndices2;
		litModel.indices3 = def.faceIndices3;
		litModel.facePriorities = def.faceRenderPriorities;
		litModel.faceTransparencies = def.faceTransparencies;
		litModel.faceTextures = def.faceTextures;

		return litModel;
	}

	static int method2608(int color, int brightness) {
		brightness = ((color & 127) * brightness) >> 7;
		return (color & 65408) + bound2to126(brightness);
	}

	static int bound2to126(int value) {
		if (value < 2) value = 2;
		else if (value > 126) value = 126;
		return value;
	}
}
