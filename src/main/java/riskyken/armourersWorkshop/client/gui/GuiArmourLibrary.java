package riskyken.armourersWorkshop.client.gui;

import java.util.ArrayList;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import riskyken.armourersWorkshop.client.gui.controls.GuiCheckBox;
import riskyken.armourersWorkshop.client.gui.controls.GuiFileListItem;
import riskyken.armourersWorkshop.client.gui.controls.GuiList;
import riskyken.armourersWorkshop.client.gui.controls.GuiScrollbar;
import riskyken.armourersWorkshop.common.equipment.data.CustomArmourItemData;
import riskyken.armourersWorkshop.common.inventory.ContainerArmourLibrary;
import riskyken.armourersWorkshop.common.lib.LibModInfo;
import riskyken.armourersWorkshop.common.network.PacketHandler;
import riskyken.armourersWorkshop.common.network.messages.MessageClientGuiLoadSaveArmour;
import riskyken.armourersWorkshop.common.tileentities.TileEntityArmourLibrary;
import cpw.mods.fml.client.config.GuiButtonExt;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuiArmourLibrary extends GuiContainer {

    private static final ResourceLocation texture = new ResourceLocation(LibModInfo.ID.toLowerCase(), "textures/gui/armourLibrary.png");
    
    private TileEntityArmourLibrary armourLibrary;
    private GuiList fileList;
    private GuiButtonExt saveButton;
    private GuiButtonExt loadButton;
    private GuiScrollbar scrollbar;
    private GuiTextField filenameTextbox;
    private GuiTextField searchTextbox;
    private GuiCheckBox checkClientFiles;
    
    public GuiArmourLibrary(InventoryPlayer invPlayer, TileEntityArmourLibrary armourLibrary) {
        super(new ContainerArmourLibrary(invPlayer, armourLibrary));
        this.armourLibrary = armourLibrary;
        this.xSize = 256;
        this.ySize = 256;
    }
    
    @Override
    public void initGui() {
        super.initGui();
        String guiName = armourLibrary.getInventoryName();
        buttonList.clear();
        saveButton = new GuiButtonExt(0, guiLeft + 156, guiTop + 96, 60, 20, GuiHelper.getLocalizedControlName(guiName, "save"));
        buttonList.add(saveButton);
        
        loadButton = new GuiButtonExt(1, guiLeft + 156, guiTop + 96 + 30, 60, 20, GuiHelper.getLocalizedControlName(guiName, "load"));
        buttonList.add(loadButton);
        
        filenameTextbox = new GuiTextField(fontRendererObj, guiLeft + 152, guiTop + 36, 96, 14);
        filenameTextbox.setMaxStringLength(24);
        
        searchTextbox = new GuiTextField(fontRendererObj, guiLeft + 7, guiTop + 36, 131, 14);
        searchTextbox.setMaxStringLength(24);
        
        fileList = new GuiList(this.guiLeft + 7, this.guiTop + 63, 131, 96, 12);
        
        scrollbar = new GuiScrollbar(2, this.guiLeft + 138, this.guiTop + 63, 10, 96, "", false);
        buttonList.add(scrollbar);
        
        checkClientFiles = new GuiCheckBox(3, this.guiLeft + 152, this.guiTop + 63, 14, 14, GuiHelper.getLocalizedControlName(guiName, "showClientFiles"), false, false);
        buttonList.add(checkClientFiles);
    }
    
    @Override
    protected void actionPerformed(GuiButton button) {
        String filename = filenameTextbox.getText().trim();
        
        if (button.id == 3) {
            checkClientFiles.setChecked(!checkClientFiles.isChecked());
        }
        
        if (!filename.equals("")) {
            switch (button.id) {
            case 0:
                PacketHandler.networkWrapper.sendToServer(new MessageClientGuiLoadSaveArmour(filename, false));
                filenameTextbox.setText("");
                break;
            case 1:
                if (checkClientFiles.isChecked()) {
                    CustomArmourItemData itemData = TileEntityArmourLibrary.loadCustomArmourItemDataFromFile(filename);
                    if (itemData != null) {
                        PacketHandler.networkWrapper.sendToServer(new MessageClientGuiLoadSaveArmour(itemData, true));
                    }
                } else {
                    PacketHandler.networkWrapper.sendToServer(new MessageClientGuiLoadSaveArmour(filename, true));
                }
                filenameTextbox.setText("");
                break;
            }
        }
        
    }
    
    @Override
    public void drawScreen(int mouseX, int mouseY, float tickTime) {
        super.drawScreen(mouseX, mouseY, tickTime);
        
        ArrayList<String> fileNames = armourLibrary.serverFileNames;
        if (checkClientFiles.isChecked()) {
            fileNames = armourLibrary.clientFileNames;
        }
        
        saveButton.enabled = !checkClientFiles.isChecked();
        
        if (fileNames!= null) {
            fileList.clearList();
            for (int i = 0; i < fileNames.size(); i++) {
                String fileName = fileNames.get(i);
                if (!searchTextbox.getText().equals("")) {
                    if (fileName.toLowerCase().contains(searchTextbox.getText().toLowerCase())) {
                        fileList.addListItem(new GuiFileListItem(fileName));
                    }
                } else {
                    fileList.addListItem(new GuiFileListItem(fileName));
                }
            }
        }
        
        fileList.setScrollPercentage(scrollbar.getPercentageValue());
    }
    
    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        super.mouseClicked(mouseX, mouseY, button);
        
        searchTextbox.mouseClicked(mouseX, mouseY, button);
        filenameTextbox.mouseClicked(mouseX, mouseY, button);
        
        if (button == 1) {
            if (searchTextbox.isFocused()) {
                searchTextbox.setText("");
            }
            if (filenameTextbox.isFocused()) {
                filenameTextbox.setText("");
            }
        }

        
        if (fileList.mouseClicked(mouseX, mouseY, button)) {
            filenameTextbox.setText(fileList.getSelectedListEntry().getDisplayName());
        }
        scrollbar.mousePressed(mc, mouseX, mouseY);
    }
    
    @Override
    protected void mouseMovedOrUp(int mouseX, int mouseY, int button) {
        super.mouseMovedOrUp(mouseX, mouseY, button);
        fileList.mouseMovedOrUp(mouseX, mouseY, button);
        scrollbar.mouseReleased(mouseX, mouseY);
    }
    
    @Override
    protected void keyTyped(char key, int keyCode) {
        if (!(searchTextbox.textboxKeyTyped(key, keyCode) | filenameTextbox.textboxKeyTyped(key, keyCode))) {
            super.keyTyped(key, keyCode);
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float someFloat,int mouseX, int mouseY) {
        GL11.glColor4f(1, 1, 1, 1);
        mc.renderEngine.bindTexture(texture);
        drawTexturedModalRect(this.guiLeft, this.guiTop, 0, 0, this.xSize, this.ySize);
        searchTextbox.drawTextBox();
        filenameTextbox.drawTextBox();
        fileList.drawList(mouseX, mouseY, 0);
    }
    
    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        GuiHelper.renderLocalizedGuiName(this.fontRendererObj, this.xSize, armourLibrary.getInventoryName());
        
        String filesLabel = GuiHelper.getLocalizedControlName(armourLibrary.getInventoryName(), "label.files");
        String filenameLabel = GuiHelper.getLocalizedControlName(armourLibrary.getInventoryName(), "label.filename");
        String searchLabel = GuiHelper.getLocalizedControlName(armourLibrary.getInventoryName(), "label.search");
        
        this.fontRendererObj.drawString(filesLabel, 7, 55, 4210752);
        this.fontRendererObj.drawString(filenameLabel, 152, 27, 4210752);
        this.fontRendererObj.drawString(searchLabel, 7, 27, 4210752);
        
        this.fontRendererObj.drawString(I18n.format("container.inventory", new Object[0]), 48, this.ySize - 96 + 2, 4210752);
    }
}
