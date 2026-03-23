package jdemic.Rendering;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.List;

import static jdemic.VulkanModules.ShaderSPIRVUtils.compileShaderFile;
import static jdemic.VulkanModules.ShaderSPIRVUtils.ShaderKind.FRAGMENT_SHADER;
import static jdemic.VulkanModules.ShaderSPIRVUtils.ShaderKind.VERTEX_SHADER;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

import jdemic.VulkanModules.ShaderSPIRVUtils.SPIRV;

public class SkydomeRenderer
{
    public static class SphereGeometry
    {
        public final int firstIndex;
        public final int indexCount;

        public SphereGeometry(int firstIndex, int indexCount)
        {
            this.firstIndex = firstIndex;
            this.indexCount = indexCount;
        }
    }

    public static <V> SphereGeometry generateSphereGeometry(List<V> generatedVertices, List<Integer> generatedIndices,
                                                            VertexFactory<V> vertexFactory, int stacks, int slices)
    {
        int firstIndex = generatedIndices.size();
        Vector3f skyColor = new Vector3f(1.0f, 1.0f, 1.0f);
        int baseVertex = generatedVertices.size();

        for(int i = 0; i <= stacks; i++)
        {
            float phi = (float) Math.PI * i / stacks;
            float sinPhi = (float) Math.sin(phi);
            float cosPhi = (float) Math.cos(phi);
            for(int j = 0; j <= slices; j++)
            {
                float theta = 2.0f * (float) Math.PI * j / slices;
                float x = sinPhi * (float) Math.cos(theta);
                float y = cosPhi;
                float z = sinPhi * (float) Math.sin(theta);
                float u = (float) j / slices;
                float v = (float) i / stacks;
                generatedVertices.add(vertexFactory.create(
                    new Vector3f(x, y, z), skyColor, new Vector2f(u, v)));
            }
        }

        for(int i = 0; i < stacks; i++)
        {
            for(int j = 0; j < slices; j++)
            {
                int first = baseVertex + i * (slices + 1) + j;
                int second = first + slices + 1;
                generatedIndices.add(first);
                generatedIndices.add(second);
                generatedIndices.add(first + 1);
                generatedIndices.add(second);
                generatedIndices.add(second + 1);
                generatedIndices.add(first + 1);
            }
        }

        int indexCount = generatedIndices.size() - firstIndex;
        return new SphereGeometry(firstIndex, indexCount);
    }

    public static long createPipeline(VkDevice device, MemoryStack stack, ByteBuffer entryPoint,
                                       VkPipelineVertexInputStateCreateInfo vertexInputInfo,
                                       VkPipelineInputAssemblyStateCreateInfo inputAssembly,
                                       VkPipelineViewportStateCreateInfo viewportState,
                                       VkPipelineMultisampleStateCreateInfo multisampling,
                                       VkPipelineColorBlendStateCreateInfo colorBlending,
                                       long pipelineLayout, long renderPass, int msaaSamples)
    {
        SPIRV skydomeVertSPIRV = compileShaderFile("shaders/skydome.vert", VERTEX_SHADER);
        SPIRV skydomeFragSPIRV = compileShaderFile("shaders/skydome.frag", FRAGMENT_SHADER);

        long skydomeVertModule = createShaderModule(device, skydomeVertSPIRV.bytecode());
        long skydomeFragModule = createShaderModule(device, skydomeFragSPIRV.bytecode());

        VkPipelineShaderStageCreateInfo.Buffer skydomeShaderStages = VkPipelineShaderStageCreateInfo.calloc(2, stack);

        VkPipelineShaderStageCreateInfo skydomeVertStage = skydomeShaderStages.get(0);
        skydomeVertStage.sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO);
        skydomeVertStage.stage(VK_SHADER_STAGE_VERTEX_BIT);
        skydomeVertStage.module(skydomeVertModule);
        skydomeVertStage.pName(entryPoint);

        VkPipelineShaderStageCreateInfo skydomeFragStage = skydomeShaderStages.get(1);
        skydomeFragStage.sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO);
        skydomeFragStage.stage(VK_SHADER_STAGE_FRAGMENT_BIT);
        skydomeFragStage.module(skydomeFragModule);
        skydomeFragStage.pName(entryPoint);

        VkPipelineDepthStencilStateCreateInfo skydomeDepthStencil = VkPipelineDepthStencilStateCreateInfo.calloc(stack);
        skydomeDepthStencil.sType(VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO);
        skydomeDepthStencil.depthTestEnable(false);
        skydomeDepthStencil.depthWriteEnable(false);
        skydomeDepthStencil.depthCompareOp(VK_COMPARE_OP_LESS_OR_EQUAL);
        skydomeDepthStencil.depthBoundsTestEnable(false);
        skydomeDepthStencil.stencilTestEnable(false);

        VkPipelineRasterizationStateCreateInfo skydomeRasterizer = VkPipelineRasterizationStateCreateInfo.calloc(stack);
        skydomeRasterizer.sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO);
        skydomeRasterizer.depthClampEnable(false);
        skydomeRasterizer.rasterizerDiscardEnable(false);
        skydomeRasterizer.polygonMode(VK_POLYGON_MODE_FILL);
        skydomeRasterizer.lineWidth(1.0f);
        skydomeRasterizer.cullMode(VK_CULL_MODE_NONE);
        skydomeRasterizer.frontFace(VK_FRONT_FACE_COUNTER_CLOCKWISE);
        skydomeRasterizer.depthBiasEnable(false);

        VkGraphicsPipelineCreateInfo.Buffer skydomePipelineInfo = VkGraphicsPipelineCreateInfo.calloc(1, stack);
        skydomePipelineInfo.sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO);
        skydomePipelineInfo.pStages(skydomeShaderStages);
        skydomePipelineInfo.pVertexInputState(vertexInputInfo);
        skydomePipelineInfo.pInputAssemblyState(inputAssembly);
        skydomePipelineInfo.pViewportState(viewportState);
        skydomePipelineInfo.pRasterizationState(skydomeRasterizer);
        skydomePipelineInfo.pMultisampleState(multisampling);
        skydomePipelineInfo.pDepthStencilState(skydomeDepthStencil);
        skydomePipelineInfo.pColorBlendState(colorBlending);
        skydomePipelineInfo.layout(pipelineLayout);
        skydomePipelineInfo.renderPass(renderPass);
        skydomePipelineInfo.subpass(0);
        skydomePipelineInfo.basePipelineHandle(VK_NULL_HANDLE);
        skydomePipelineInfo.basePipelineIndex(-1);

        LongBuffer pSkydomePipeline = stack.mallocLong(1);

        if(vkCreateGraphicsPipelines(device, VK_NULL_HANDLE, skydomePipelineInfo, null, pSkydomePipeline) != VK_SUCCESS)
        {
            throw new RuntimeException("Failed to create skydome pipeline");
        }

        long pipeline = pSkydomePipeline.get(0);

        vkDestroyShaderModule(device, skydomeVertModule, null);
        vkDestroyShaderModule(device, skydomeFragModule, null);
        skydomeVertSPIRV.free();
        skydomeFragSPIRV.free();

        return pipeline;
    }

    public static void recordDrawCommands(VkCommandBuffer commandBuffer, MemoryStack stack,
                                          long skydomePipeline, long pipelineLayout,
                                          long descriptorSet, int firstIndex, int indexCount)
    {
        vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, skydomePipeline);
        vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS,
                                pipelineLayout, 0, stack.longs(descriptorSet), null);
        vkCmdDrawIndexed(commandBuffer, indexCount, 1, firstIndex, 0, 0);
    }

    private static long createShaderModule(VkDevice device, ByteBuffer spirvCode)
    {
        try(MemoryStack stack = stackPush())
        {
            VkShaderModuleCreateInfo createInfo = VkShaderModuleCreateInfo.calloc(stack);
            createInfo.sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO);
            createInfo.pCode(spirvCode);

            LongBuffer pShaderModule = stack.mallocLong(1);

            if(vkCreateShaderModule(device, createInfo, null, pShaderModule) != VK_SUCCESS)
            {
                throw new RuntimeException("Failed to create shader module");
            }

            return pShaderModule.get(0);
        }
    }

    @FunctionalInterface
    public interface VertexFactory<V>
    {
        V create(Vector3f pos, Vector3f color, Vector2f texCoords);
    }
}
