package riskyken.armourersWorkshop.common.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import riskyken.armourersWorkshop.ArmourersWorkshop;
import riskyken.armourersWorkshop.common.items.block.ModItemBlock;
import riskyken.armourersWorkshop.common.lib.LibBlockNames;
import riskyken.armourersWorkshop.common.lib.LibGuiIds;
import riskyken.armourersWorkshop.common.lib.LibModInfo;
import riskyken.armourersWorkshop.common.tileentities.TileEntityColourMixer;
import riskyken.armourersWorkshop.proxies.ClientProxy;
import riskyken.armourersWorkshop.utils.UtilBlocks;
import cpw.mods.fml.common.network.internal.FMLNetworkHandler;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class BlockColourMixer extends AbstractModBlock implements ITileEntityProvider {

    public BlockColourMixer() {
        super(LibBlockNames.COLOUR_MIXER);
    }
    
    @Override
    public Block setBlockName(String name) {
        GameRegistry.registerBlock(this, ModItemBlock.class, "block." + name);
        return super.setBlockName(name);
    }
    
    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        UtilBlocks.dropInventoryBlocks(world, x, y, z);
        super.breakBlock(world, x, y, z, block, meta);
    }
    
    @SideOnly(Side.CLIENT)
    private IIcon topIcon;
    @SideOnly(Side.CLIENT)
    private IIcon bottomIcon;
    @SideOnly(Side.CLIENT)
    private IIcon sideOverlayIcon;

    @SideOnly(Side.CLIENT)
    @Override
    public void registerBlockIcons(IIconRegister register) {
        blockIcon = register.registerIcon(LibModInfo.ID.toLowerCase() + ":"
                + "colourMixerSide");
        topIcon = register.registerIcon(LibModInfo.ID.toLowerCase() + ":"
                + "colourMixerTop");
        bottomIcon = register.registerIcon(LibModInfo.ID.toLowerCase() + ":"
                + "colourMixerBottom");
        sideOverlayIcon = register.registerIcon(LibModInfo.ID.toLowerCase() + ":"
                + "colourMixerSideOverlay");
    }
    
    @SideOnly(Side.CLIENT)
    @Override
    public IIcon getIcon(int side, int meta) {
        if (side == 0) { return bottomIcon; }
        if (side == 1) { return topIcon; }
        
        if (ClientProxy.renderPass == 0) {
            return sideOverlayIcon;
        }
        
        return blockIcon;
    }
    
    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }
    
    @SideOnly(Side.CLIENT)
    @Override
    public int colorMultiplier(IBlockAccess blockAccess, int x, int y, int z) {
        if (ClientProxy.renderPass == 0) {
            TileEntity te = blockAccess.getTileEntity(x, y, z);
            if (te != null && te instanceof TileEntityColourMixer) {
                return ((TileEntityColourMixer)te).getColour();
            }
        }
        return 16777215;
    }
    
    @Override
    public int getRenderType() {
        return ArmourersWorkshop.proxy.getRenderType(this);
    }
    
    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float xHit, float yHit, float zHit) {
        if (!world.isRemote) {
            FMLNetworkHandler.openGui(player, ArmourersWorkshop.instance, LibGuiIds.COLOUR_MIXER, world, x, y, z);
        }
        return true;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int p_149915_2_) {
        return new TileEntityColourMixer();
    }
}
