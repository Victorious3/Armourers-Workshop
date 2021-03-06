package riskyken.armourersWorkshop.common.equipment;

import java.awt.Color;
import java.util.BitSet;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;
import net.minecraftforge.common.IExtendedEntityProperties;
import net.minecraftforge.common.util.Constants.NBT;
import riskyken.armourersWorkshop.api.common.equipment.EnumEquipmentType;
import riskyken.armourersWorkshop.api.common.equipment.IEntityEquipment;
import riskyken.armourersWorkshop.api.common.lib.LibCommonTags;
import riskyken.armourersWorkshop.common.equipment.data.CustomArmourItemData;
import riskyken.armourersWorkshop.common.handler.EquipmentDataHandler;
import riskyken.armourersWorkshop.common.items.ItemColourPicker;
import riskyken.armourersWorkshop.common.items.ModItems;
import riskyken.armourersWorkshop.common.network.PacketHandler;
import riskyken.armourersWorkshop.common.network.messages.MessageServerAddEquipmentInfo;
import riskyken.armourersWorkshop.common.network.messages.MessageServerUpdateSkinInfo;
import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;

public class ExtendedPropsPlayerEquipmentData implements IExtendedEntityProperties, IInventory {

    public static final String TAG_EXT_PROP_NAME = "playerCustomEquipmentData";
    private static final String TAG_ITEMS = "items";
    private static final String TAG_SLOT = "slot";
    private static final String TAG_NAKED = "naked";
    private static final String TAG_SKIN_COLOUR = "skinColour";
    private static final String TAG_PANTS_COLOUR = "pantsColour";
    private static final String TAG_ARMOUR_OVERRIDE = "armourOverride";
    private static final String TAG_HEAD_OVERLAY = "headOverlay";
    
    public ItemStack[] customArmourInventory = new ItemStack[8];
    private EntityEquipmentData equipmentData = new EntityEquipmentData();
    private final EntityPlayer player;
    private boolean inventoryChanged;
    
    private boolean isNaked = false;
    private int skinColour = Color.decode("#F9DFD2").getRGB();
    private int pantsColour = Color.decode("#FCFCFC").getRGB();
    private BitSet armourOverride = new BitSet(4);
    boolean headOverlay;
    
    public ExtendedPropsPlayerEquipmentData(EntityPlayer player) {
        this.player = player;
    }
    
    public static final void register(EntityPlayer player) {
        player.registerExtendedProperties(ExtendedPropsPlayerEquipmentData.TAG_EXT_PROP_NAME, new ExtendedPropsPlayerEquipmentData(player));
    }
    
    public static final ExtendedPropsPlayerEquipmentData get(EntityPlayer player) {
        return (ExtendedPropsPlayerEquipmentData) player.getExtendedProperties(TAG_EXT_PROP_NAME);
    }
    
    public void setEquipmentStack(ItemStack stack) {
        EnumEquipmentType equipmentType = EquipmentDataHandler.INSTANCE.getEquipmentTypeFromStack(stack);
        switch (equipmentType) {
        case HEAD:
            setInventorySlotContents(0, stack);
            break;
        case CHEST:
            setInventorySlotContents(1, stack);
            break;
        case LEGS:
            setInventorySlotContents(2, stack);
            break;
        case SKIRT:
            setInventorySlotContents(3, stack);
            break;
        case FEET:
            setInventorySlotContents(4, stack);
            break;
        case WEAPON:
            setInventorySlotContents(5, stack);
            break;
        default:
            break;
        }
    }
    
    public ItemStack[] getAllEquipmentStacks() {
        return customArmourInventory;
    }
    
    public ItemStack getEquipmentStack(EnumEquipmentType equipmentType) {
        switch (equipmentType) {
        case HEAD:
            return getStackInSlot(0);
        case CHEST:
            return getStackInSlot(1);
        case LEGS:
            return getStackInSlot(2);
        case SKIRT:
            return getStackInSlot(3);
        case FEET:
            return getStackInSlot(4);
        case WEAPON:
            return getStackInSlot(5);
        default:
            return null;
        }
    }
    
    public void clearAllEquipmentStacks() {
        for (int i = 0; i < 6; i++) {
            setInventorySlotContents(i, null);
        }
    }
    
    public void clearEquipmentStack(EnumEquipmentType equipmentType) {
        switch (equipmentType) {
        case HEAD:
            setInventorySlotContents(0, null);
            break;
        case CHEST:
            setInventorySlotContents(1, null);
            break;
        case LEGS:
            setInventorySlotContents(2, null);
            break;
        case SKIRT:
            setInventorySlotContents(3, null);
            break;
        case FEET:
            setInventorySlotContents(4, null);
            break;
        case WEAPON:
            setInventorySlotContents(5, null);
            break;
        default:
            break;
        }
    }
    
    public void addCustomEquipment(EnumEquipmentType type, int equipmentId) {
        equipmentData.addEquipment(type, equipmentId);
        updateEquipmentDataToPlayersAround();
    }

    public void removeCustomEquipment(EnumEquipmentType type) {
        equipmentData.removeEquipment(type);
        updateEquipmentDataToPlayersAround();
    }
    
    public void removeAllCustomEquipment() {
        equipmentData.removeEquipment(EnumEquipmentType.HEAD);
        equipmentData.removeEquipment(EnumEquipmentType.CHEST);
        equipmentData.removeEquipment(EnumEquipmentType.LEGS);
        equipmentData.removeEquipment(EnumEquipmentType.SKIRT);
        equipmentData.removeEquipment(EnumEquipmentType.FEET);
        equipmentData.removeEquipment(EnumEquipmentType.WEAPON);
        updateEquipmentDataToPlayersAround();
    }
    
    private void updateEquipmentDataToPlayersAround() {
        TargetPoint p = new TargetPoint(player.dimension, player.posX, player.posY, player.posZ, 512);
        PacketHandler.networkWrapper.sendToAllAround(new MessageServerAddEquipmentInfo(player.getPersistentID(), equipmentData), p);
    }
    
    public void colourSlotUpdate(byte slot) {
        ItemStack stackInput = this.getStackInSlot(slot);
        ItemStack stackOutput = this.getStackInSlot(slot + 1);
        
        if (stackInput != null && stackInput.getItem() == ModItems.colourPicker && stackOutput == null) {
            //Silliness!
            if (stackInput.getDisplayName().toLowerCase().equals("panties!")) {
                this.pantsColour = ((ItemColourPicker)stackInput.getItem()).getToolColour(stackInput);
            } else {
                this.skinColour = ((ItemColourPicker)stackInput.getItem()).getToolColour(stackInput);
            }
            
            setInventorySlotContents(slot + 1, stackInput);
            setInventorySlotContents(slot, null);
            sendSkinData();
        }
    }

    public void setEquipmentData(IEntityEquipment equipmentData) {
        for (int i = 1; i < EnumEquipmentType.values().length; i++) {
            EnumEquipmentType equipmentType = EnumEquipmentType.getOrdinal(i);
            if (equipmentData.haveEquipment(equipmentType)) {
                setEquipment(equipmentType, equipmentData.getEquipmentId(equipmentType));
            } else {
                clearEquipment(equipmentType);
            }
        }
        //updateEquipmentDataToPlayersAround();
    }
    
    private void setEquipment(EnumEquipmentType equipmentType, int equipmentId) {
        ItemStack stack = EquipmentDataHandler.INSTANCE.getCustomEquipmentItemStack(equipmentId);
        setEquipment(equipmentType, stack);
    }
    
    private void clearEquipment(EnumEquipmentType equipmentType) {
        setEquipment(equipmentType, null);
    }
    
    private void setEquipment(EnumEquipmentType equipmentType, ItemStack stack) {
        
        switch (equipmentType) {
        case NONE:
            break;
        case HEAD:
            setInventorySlotContents(0, stack);
            break;
        case CHEST:
            setInventorySlotContents(1, stack);
            break;
        case LEGS:
            setInventorySlotContents(2, stack);
            break;
        case SKIRT:
            setInventorySlotContents(3, stack);
            break;
        case FEET:
            setInventorySlotContents(4, stack);
            break;
        case WEAPON:
            setInventorySlotContents(5, stack);
            break;
        }
    }
    
    public EntityEquipmentData getEquipmentData() {
        return equipmentData;
    }
    
    public void armourSlotUpdate(byte slot) {
        ItemStack stack = this.getStackInSlot(slot);
        
        if (stack == null) {
            removeArmourFromSlot(slot);
            return;
        } 
        
        if (!stack.hasTagCompound()) { return; }
        
        NBTTagCompound data = stack.getTagCompound();
        if (!data.hasKey(LibCommonTags.TAG_ARMOUR_DATA)) { return ;}
        NBTTagCompound armourNBT = data.getCompoundTag(LibCommonTags.TAG_ARMOUR_DATA);
        loadFromItemNBT(armourNBT);
    }
    
    private void removeArmourFromSlot(byte slotId) {
        switch (slotId) {
        case 5:
            removeCustomEquipment(EnumEquipmentType.WEAPON);
            break;
        case 4:
            removeCustomEquipment(EnumEquipmentType.FEET);
            break;
        case 3:
            removeCustomEquipment(EnumEquipmentType.SKIRT);
            break;  
        case 2:
            removeCustomEquipment(EnumEquipmentType.LEGS);
            break;
        case 1:
            removeCustomEquipment(EnumEquipmentType.CHEST);
            break;
        case 0:
            removeCustomEquipment(EnumEquipmentType.HEAD);
            break;
        }
    }
    
    public void removeAllCustomArmourData() {
        player.addChatMessage(new ChatComponentText("You're custom armour data was cleared."));
        removeCustomEquipment(EnumEquipmentType.HEAD);
        removeCustomEquipment(EnumEquipmentType.CHEST);
        removeCustomEquipment(EnumEquipmentType.LEGS);
        removeCustomEquipment(EnumEquipmentType.SKIRT);
        removeCustomEquipment(EnumEquipmentType.FEET);
        removeCustomEquipment(EnumEquipmentType.WEAPON);
    }
    
    public void sendCustomArmourDataToPlayer(EntityPlayerMP targetPlayer) {
        checkAndSendCustomArmourDataTo(targetPlayer);
        sendNakedData(targetPlayer);
    }
    
    public void setSkinInfo(boolean naked, int skinColour, int pantsColour, BitSet armourOverride, boolean headOverlay) {
        this.isNaked = naked;
        this.skinColour = skinColour;
        this.pantsColour = pantsColour;
        this.armourOverride = armourOverride;
        this.headOverlay = headOverlay;
        sendSkinData();
    }
    
    private void checkAndSendCustomArmourDataTo(EntityPlayerMP targetPlayer) {
        PacketHandler.networkWrapper.sendTo(new MessageServerAddEquipmentInfo(player.getUniqueID(), equipmentData), targetPlayer);
    }
    
    private void sendNakedData(EntityPlayerMP targetPlayer) {
        PacketHandler.networkWrapper.sendTo(new MessageServerUpdateSkinInfo(this.player.getUniqueID(), this.isNaked, this.skinColour, this.pantsColour, armourOverride, headOverlay), targetPlayer);
    }
    
    private void sendSkinData() {
        TargetPoint p = new TargetPoint(player.dimension, player.posX, player.posY, player.posZ, 512);
        PacketHandler.networkWrapper.sendToAllAround(new MessageServerUpdateSkinInfo(this.player.getUniqueID(), this.isNaked, this.skinColour, this.pantsColour, armourOverride, headOverlay), p);
    }
    
    public BitSet getArmourOverride() {
        return armourOverride;
    }
    
    @Override
    public void saveNBTData(NBTTagCompound compound) {
        writeItemsToNBT(compound);
        compound.setBoolean(TAG_NAKED, this.isNaked);
        compound.setInteger(TAG_SKIN_COLOUR, this.skinColour);
        compound.setInteger(TAG_PANTS_COLOUR, this.pantsColour);
        for (int i = 0; i < 4; i++) {
        	compound.setBoolean(TAG_ARMOUR_OVERRIDE + i, this.armourOverride.get(i));
        }
        compound.setBoolean(TAG_HEAD_OVERLAY, this.headOverlay);
    }
    
    @Override
    public void loadNBTData(NBTTagCompound compound) {
        readItemsFromNBT(compound);
        this.isNaked = compound.getBoolean(TAG_NAKED);
        if (compound.hasKey(TAG_SKIN_COLOUR)) {
            this.skinColour = compound.getInteger(TAG_SKIN_COLOUR);
        }
        if (compound.hasKey(TAG_PANTS_COLOUR)) {
            this.pantsColour = compound.getInteger(TAG_PANTS_COLOUR);
        }
        for (int i = 0; i < 4; i++) {
        	this.armourOverride.set(i, compound.getBoolean(TAG_ARMOUR_OVERRIDE + i));
        }
        this.headOverlay = compound.getBoolean(TAG_HEAD_OVERLAY);
    }
    
    private void loadFromItemNBT(NBTTagCompound compound) {
        int equipmentId = compound.getInteger(LibCommonTags.TAG_EQUIPMENT_ID);
        CustomArmourItemData equipmentData = EquipmentDataCache.INSTANCE.getEquipmentData(equipmentId);
        
        addCustomEquipment(equipmentData.getType(), equipmentId);
    }
    
    @Override
    public void init(Entity entity, World world) {
        
    }

    @Override
    public int getSizeInventory() {
        return customArmourInventory.length;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return customArmourInventory[slot];
    }

    @Override
    public ItemStack decrStackSize(int slot, int count) {
        ItemStack itemstack = getStackInSlot(slot);
        
        if (itemstack != null) {
            if (itemstack.stackSize <= count){
                setInventorySlotContents(slot, null);
            }else{
                itemstack = itemstack.splitStack(count);
                markDirty();
            }
        }
        return itemstack;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
        ItemStack item = getStackInSlot(slot);
        setInventorySlotContents(slot, null);
        return item;
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        customArmourInventory[slot] = stack;
        if (stack != null && stack.stackSize > getInventoryStackLimit()) {
            stack.stackSize = getInventoryStackLimit();
        }
        if (!player.worldObj.isRemote) {
            if (slot < 6) {
                armourSlotUpdate((byte)slot);
            }

        }

        colourSlotUpdate((byte)6);
        
        markDirty();
    }

    @Override
    public String getInventoryName() {
        return "pasta";
    }

    @Override
    public boolean hasCustomInventoryName() {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public void markDirty() {
        this.inventoryChanged = true;
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        return this.player.isDead ? false : player.getDistanceSqToEntity(this.player) <= 64.0D;
    }

    @Override
    public void openInventory() {}

    @Override
    public void closeInventory() {}

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return true;
    }
    
    public void writeItemsToNBT(NBTTagCompound compound) {
        NBTTagList items = new NBTTagList();
        for (int i = 0; i < getSizeInventory(); i++) {
            ItemStack stack = getStackInSlot(i);
            if (stack != null) {
                NBTTagCompound item = new NBTTagCompound();
                item.setByte(TAG_SLOT, (byte)i);
                stack.writeToNBT(item);
                items.appendTag(item);
            }
        }
        compound.setTag(TAG_ITEMS, items);
    }
    
    public void readItemsFromNBT(NBTTagCompound compound) {
        NBTTagList items = compound.getTagList(TAG_ITEMS, NBT.TAG_COMPOUND);
        for (int i = 0; i < items.tagCount(); i++) {
            NBTTagCompound item = (NBTTagCompound)items.getCompoundTagAt(i);
            int slot = item.getByte(TAG_SLOT);
            
            if (slot >= 0 && slot < getSizeInventory()) {
                setInventorySlotContents(slot, ItemStack.loadItemStackFromNBT(item));
            }
        }
    }
    
    public void dropItems() {
        World world = player.worldObj;
        double x = player.posX;
        double y = player.posY;
        double z = player.posZ;
        for (int i = 0; i < this.getSizeInventory(); i++) {
            ItemStack stack = this.getStackInSlot(i);
            if (stack != null) {
                float f = 0.7F;
                double xV = (double)(world.rand.nextFloat() * f) + (double)(1.0F - f) * 0.5D;
                double yV = (double)(world.rand.nextFloat() * f) + (double)(1.0F - f) * 0.5D;
                double zV = (double)(world.rand.nextFloat() * f) + (double)(1.0F - f) * 0.5D;
                EntityItem entityitem = new EntityItem(world, (double)x + xV, (double)y + yV, (double)z + zV, stack);
                world.spawnEntityInWorld(entityitem);
                setInventorySlotContents(i, null);
            }
        }
    }
}
