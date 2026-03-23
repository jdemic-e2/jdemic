import jdemic.VulkanModules.Frame;
import jdemic.VulkanModules.ShaderSPIRVUtils.SPIRV;
import jdemic.GameLogic.CityNode;
import jdemic.GameLogic.PandemicMapGraph;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Pointer;
import org.lwjgl.vulkan.*;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.geom.RoundRectangle2D;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;
import static jdemic.VulkanModules.AlignmentUtils.alignas;
import static jdemic.VulkanModules.AlignmentUtils.alignof;
import static jdemic.VulkanModules.ShaderSPIRVUtils.compileShaderFile;
import static jdemic.VulkanModules.ShaderSPIRVUtils.ShaderKind.FRAGMENT_SHADER;
import static jdemic.VulkanModules.ShaderSPIRVUtils.ShaderKind.VERTEX_SHADER;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFWVulkan.glfwCreateWindowSurface;
import static org.lwjgl.glfw.GLFWVulkan.glfwGetRequiredInstanceExtensions;
import static org.lwjgl.stb.STBImage.*;
import static org.lwjgl.system.Configuration.DEBUG;
import static org.lwjgl.system.MemoryStack.stackGet;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memAlloc;
import static org.lwjgl.system.MemoryUtil.memFree;
import static org.lwjgl.vulkan.EXTDebugUtils.*;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

class jDemicEngine
{

    private static class jDemicApp
    {

        private static final int UINT32_MAX = 0xFFFFFFFF;
        private static final long UINT64_MAX = 0xFFFFFFFFFFFFFFFFL;

        private static final int WIDTH = 800;
        private static final int HEIGHT = 600;

        private static final int MAX_FRAMES_IN_FLIGHT = 2;
        private static final String PANDEMIC_MAP_TEXTURE_PATH = "textures/pandemicmap.png";
        private static final float MAP_NODE_ALIGNMENT_OFFSET_X = 0.006f;
        private static final float MAP_NODE_ALIGNMENT_OFFSET_Y = -0.004f;
        private static final float MAP_HOVER_RADIUS_PX = 18.0f;
        private static final int MAP_NODE_RADIUS_PX = 5*5;
        private static final int MAP_NODE_HOVER_RADIUS_PX = 8*5;
        private static final float MAP_BORDER_FRACTION = 0.025f;

        private static final boolean ENABLE_VALIDATION_LAYERS = DEBUG.get(true);

        private static final Set<String> VALIDATION_LAYERS;
        static
        {
            if(ENABLE_VALIDATION_LAYERS)
            {
                VALIDATION_LAYERS = new HashSet<>();
                VALIDATION_LAYERS.add("VK_LAYER_KHRONOS_validation");
            }
            else
            {
                // We are not going to use it, so we don't create it
                VALIDATION_LAYERS = null;
            }
        }

        private static final Set<String> DEVICE_EXTENSIONS = Stream.of(VK_KHR_SWAPCHAIN_EXTENSION_NAME)
            .collect(toSet());



        private static int debugCallback(int messageSeverity, int messageType, long pCallbackData, long pUserData)
        {

            VkDebugUtilsMessengerCallbackDataEXT callbackData = VkDebugUtilsMessengerCallbackDataEXT.create(pCallbackData);

            System.err.println("Validation layer: " + callbackData.pMessageString());

            return VK_FALSE;
        }

        private static int createDebugUtilsMessengerEXT(VkInstance instance, VkDebugUtilsMessengerCreateInfoEXT createInfo,
                VkAllocationCallbacks allocationCallbacks, LongBuffer pDebugMessenger)
        {

            if(vkGetInstanceProcAddr(instance, "vkCreateDebugUtilsMessengerEXT") != NULL)
            {
                return vkCreateDebugUtilsMessengerEXT(instance, createInfo, allocationCallbacks, pDebugMessenger);
            }

            return VK_ERROR_EXTENSION_NOT_PRESENT;
        }

        private static void destroyDebugUtilsMessengerEXT(VkInstance instance, long debugMessenger, VkAllocationCallbacks allocationCallbacks)
        {

            if(vkGetInstanceProcAddr(instance, "vkDestroyDebugUtilsMessengerEXT") != NULL)
            {
                vkDestroyDebugUtilsMessengerEXT(instance, debugMessenger, allocationCallbacks);
            }

        }

        private class QueueFamilyIndices
        {

            // We use Integer to use null as the empty value
            private Integer graphicsFamily;
            private Integer presentFamily;

            private boolean isComplete()
            {
                return graphicsFamily != null && presentFamily != null;
            }

            public int[] unique()
            {
                return IntStream.of(graphicsFamily, presentFamily).distinct().toArray();
            }
        }

        private class SwapChainSupportDetails
        {

            private VkSurfaceCapabilitiesKHR capabilities;
            private VkSurfaceFormatKHR.Buffer formats;
            private IntBuffer presentModes;

        }

        private static class UniformBufferObject
        {

            private static final int SIZEOF = (3 * 16 + 4 + 7) * Float.BYTES;

            private Matrix4f model;
            private Matrix4f view;
            private Matrix4f proj;
            private float time;
            private float hoverU;
            private float hoverV;
            private float hoverRadiusU;
            private float hoverRadiusV;
            private float hoverColorR;
            private float hoverColorG;
            private float hoverColorB;

            public UniformBufferObject()
            {
                model = new Matrix4f();
                view = new Matrix4f();
                proj = new Matrix4f();
            }
        }

        private static class TextureResource
        {
            private final long image;
            private final long memory;
            private final int mipLevels;

            private TextureResource(long image, long memory, int mipLevels)
            {
                this.image = image;
                this.memory = memory;
                this.mipLevels = mipLevels;
            }
        }

        private static class LoadedTextureData
        {
            private final ByteBuffer pixels;
            private final int width;
            private final int height;
            private final boolean stbOwned;

            private LoadedTextureData(ByteBuffer pixels, int width, int height, boolean stbOwned)
            {
                this.pixels = pixels;
                this.width = width;
                this.height = height;
                this.stbOwned = stbOwned;
            }
        }

        private static class AlignedCityNode
        {
            private final CityNode city;
            private final int pixelX;
            private final int pixelY;
            private final float mapU;
            private final float mapV;

            private AlignedCityNode(CityNode city, int pixelX, int pixelY, int imageWidth, int imageHeight)
            {
                this.city = city;
                this.pixelX = pixelX;
                this.pixelY = pixelY;
                this.mapU = (float) pixelX / (float) imageWidth;
                this.mapV = (float) pixelY / (float) imageHeight;
            }
        }

        private static class Vertex
        {

            private static final int SIZEOF = (3 + 3 + 2) * Float.BYTES;
            private static final int OFFSETOF_POS = 0;
            private static final int OFFSETOF_COLOR = 3 * Float.BYTES;
            private static final int OFFSETOF_TEXTCOORDS = (3 + 3) * Float.BYTES;

            private Vector3fc pos;
            private Vector3fc color;
            private Vector2fc texCoords;

            public Vertex(Vector3fc pos, Vector3fc color, Vector2fc texCoords)
            {
                this.pos = pos;
                this.color = color;
                this.texCoords = texCoords;
            }

            private static VkVertexInputBindingDescription.Buffer getBindingDescription(MemoryStack stack)
            {

                VkVertexInputBindingDescription.Buffer bindingDescription =
                    VkVertexInputBindingDescription.calloc(1, stack);

                bindingDescription.binding(0);
                bindingDescription.stride(Vertex.SIZEOF);
                bindingDescription.inputRate(VK_VERTEX_INPUT_RATE_VERTEX);

                return bindingDescription;
            }

            private static VkVertexInputAttributeDescription.Buffer getAttributeDescriptions(MemoryStack stack)
            {

                VkVertexInputAttributeDescription.Buffer attributeDescriptions =
                    VkVertexInputAttributeDescription.calloc(3, stack);

                // Position
                VkVertexInputAttributeDescription posDescription = attributeDescriptions.get(0);
                posDescription.binding(0);
                posDescription.location(0);
                posDescription.format(VK_FORMAT_R32G32B32_SFLOAT);
                posDescription.offset(OFFSETOF_POS);

                // Color
                VkVertexInputAttributeDescription colorDescription = attributeDescriptions.get(1);
                colorDescription.binding(0);
                colorDescription.location(1);
                colorDescription.format(VK_FORMAT_R32G32B32_SFLOAT);
                colorDescription.offset(OFFSETOF_COLOR);

                // Texture coordinates
                VkVertexInputAttributeDescription texCoordsDescription = attributeDescriptions.get(2);
                texCoordsDescription.binding(0);
                texCoordsDescription.location(2);
                texCoordsDescription.format(VK_FORMAT_R32G32_SFLOAT);
                texCoordsDescription.offset(OFFSETOF_TEXTCOORDS);

                return attributeDescriptions.rewind();
            }

        }

        // ======= FIELDS ======= //

        private long window;

        private VkInstance instance;
        private long debugMessenger;
        private long surface;

        private VkPhysicalDevice physicalDevice;
        private int msaaSamples = VK_SAMPLE_COUNT_1_BIT;
        private VkDevice device;

        private VkQueue graphicsQueue;
        private VkQueue presentQueue;

        private long swapChain;
        private List<Long> swapChainImages;
        private int swapChainImageFormat;
        private VkExtent2D swapChainExtent;
        private List<Long> swapChainImageViews;
        private List<Long> swapChainFramebuffers;

        private long renderPass;
        private long descriptorPool;
        private long descriptorSetLayout;
        private List<Long> descriptorSetsWood;
        private List<Long> descriptorSetsMap;
        private long pipelineLayout;
        private long graphicsPipeline;

        private long commandPool;

        private long colorImage;
        private long colorImageMemory;
        private long colorImageView;

        private long depthImage;
        private long depthImageMemory;
        private long depthImageView;

        private int woodMipLevels;
        private long woodTextureImage;
        private long woodTextureImageMemory;
        private long woodTextureImageView;
        private int mapMipLevels;
        private long mapTextureImage;
        private long mapTextureImageMemory;
        private long mapTextureImageView;
        private long textureSampler;

        private Vertex[] vertices;
        private int[] indices;
        private long vertexBuffer;
        private long vertexBufferMemory;
        private long indexBuffer;
        private long indexBufferMemory;
        private int woodIndexCount;
        private int mapPanelFirstIndex;
        private int mapPanelIndexCount;

        private List<Long> uniformBuffers;
        private List<Long> uniformBuffersMemory;

        private List<VkCommandBuffer> commandBuffers;

        private List<Frame> inFlightFrames;
        private List<Long> renderFinishedSemaphoresByImage;
        private Map<Integer, Frame> imagesInFlight;
        private int currentFrame;

        boolean framebufferResize;
        private boolean mapFocusMode;
        private boolean tabPressed;
        private boolean leftClickPressed;
        private List<CityNode> pandemicCities;
        private List<AlignedCityNode> alignedCityNodes;
        private BufferedImage pandemicMapBaseImage;
        private int mapTextureWidth;
        private int mapTextureHeight;
        private int hoveredCityIndex = -1;

        // ======= METHODS ======= //

        public void run()
        {
            initWindow();
            initVulkan();
            mainLoop();
            cleanup();
        }

        private void initWindow()
        {

            if(!glfwInit())
            {
                throw new RuntimeException("Cannot initialize GLFW");
            }

            glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);

            String title = getClass().getEnclosingClass().getSimpleName();

            window = glfwCreateWindow(WIDTH, HEIGHT, title, NULL, NULL);

            if(window == NULL)
            {
                throw new RuntimeException("Cannot create window");
            }

            // In Java, we don't really need a user pointer here, because
            // we can simply pass an instance method reference to glfwSetFramebufferSizeCallback
            glfwSetFramebufferSizeCallback(window, this::framebufferResizeCallback);
        }

        private void framebufferResizeCallback(long window, int width, int height)
        {
            // jDemicApp app = MemoryUtil.memGlobalRefToObject(glfwGetWindowUserPointer(window));
            // app.framebufferResize = true;
            framebufferResize = true;
        }

        private void initVulkan()
        {
            createInstance();
            setupDebugMessenger();
            createSurface();
            pickPhysicalDevice();
            createLogicalDevice();
            createCommandPool();
            createTextureImages();
            createTextureImageViews();
            createTextureSampler();
            loadModel();
            createVertexBuffer();
            createIndexBuffer();
            createDescriptorSetLayout();
            createSwapChainObjects();
            createSyncObjects();
        }

        private void mainLoop()
        {

            while(!glfwWindowShouldClose(window))
            {
                glfwPollEvents();
                updateInputState();
                drawFrame();
            }

            // Wait for the device to complete all operations before release resources
            vkDeviceWaitIdle(device);
        }

        private void updateInputState()
        {
            boolean isTabDown = glfwGetKey(window, GLFW_KEY_TAB) == GLFW_PRESS;
            if(isTabDown && !tabPressed)
            {
                mapFocusMode = !mapFocusMode;
            }
            tabPressed = isTabDown;

            boolean isLeftClickDown = glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS;
            int hoveredCity = detectHoveredCityIndex();

            if(isLeftClickDown && !leftClickPressed && hoveredCity != -1)
            {
                var City = alignedCityNodes.get(hoveredCity).city;
                City.clickEvent();
            }
            leftClickPressed = isLeftClickDown;

            updateHoveredCityState();
        }

        private void updateHoveredCityState()
        {
            if(alignedCityNodes == null || alignedCityNodes.isEmpty())
            {
                return;
            }

            hoveredCityIndex = detectHoveredCityIndex();
        }

        private int detectHoveredCityIndex()
        {
            try(MemoryStack stack = stackPush())
            {
                DoubleBuffer cursorX = stack.mallocDouble(1);
                DoubleBuffer cursorY = stack.mallocDouble(1);
                glfwGetCursorPos(window, cursorX, cursorY);

                IntBuffer windowWidth = stack.mallocInt(1);
                IntBuffer windowHeight = stack.mallocInt(1);
                IntBuffer framebufferWidth = stack.mallocInt(1);
                IntBuffer framebufferHeight = stack.mallocInt(1);
                glfwGetWindowSize(window, windowWidth, windowHeight);
                glfwGetFramebufferSize(window, framebufferWidth, framebufferHeight);

                if(windowWidth.get(0) == 0 || windowHeight.get(0) == 0)
                {
                    return -1;
                }

                float cursorFramebufferX = (float) (cursorX.get(0) * framebufferWidth.get(0) / (double) windowWidth.get(0));
                float cursorFramebufferY = (float) (cursorY.get(0) * framebufferHeight.get(0) / (double) windowHeight.get(0));

                Matrix4f model = new Matrix4f().identity();
                Matrix4f view = buildCurrentViewMatrix();
                Matrix4f projection = buildCurrentProjectionMatrix();
                Matrix4f mvp = new Matrix4f(projection).mul(view).mul(model);

                int bestIndex = -1;
                float bestDistanceSquared = MAP_HOVER_RADIUS_PX * MAP_HOVER_RADIUS_PX;

                for(int i = 0; i < alignedCityNodes.size(); i++)
                {
                    AlignedCityNode alignedNode = alignedCityNodes.get(i);
                    Vector2f screen = projectNodeToScreen(alignedNode, mvp);
                    if(screen == null)
                    {
                        continue;
                    }

                    float dx = screen.x - cursorFramebufferX;
                    float dy = screen.y - cursorFramebufferY;
                    float distanceSquared = (dx * dx) + (dy * dy);
                    if(distanceSquared < bestDistanceSquared)
                    {
                        bestDistanceSquared = distanceSquared;
                        bestIndex = i;
                    }
                }

                return bestIndex;
            }
        }

        

        private Vector2f projectNodeToScreen(AlignedCityNode alignedNode, Matrix4f mvp)
        {
            // Adjust U/V to account for checkerboard border inset
            float insetU = MAP_BORDER_FRACTION + alignedNode.mapU * (1.0f - 2.0f * MAP_BORDER_FRACTION);
            float insetV = MAP_BORDER_FRACTION + alignedNode.mapV * (1.0f - 2.0f * MAP_BORDER_FRACTION);
            float worldX = -1.00f + (insetU * 2.00f);
            float worldY = 0.145f;
            float worldZ = -0.52f + (insetV * 1.04f);

            Vector4f clip = new Vector4f(worldX, worldY, worldZ, 1.0f);
            mvp.transform(clip);

            if(clip.w <= 0.0f)
            {
                return null;
            }

            float ndcX = clip.x / clip.w;
            float ndcY = clip.y / clip.w;

            float screenX = (ndcX * 0.5f + 0.5f) * swapChainExtent.width();
            float screenY = (ndcY * 0.5f + 0.5f) * swapChainExtent.height();

            return new Vector2f(screenX, screenY);
        }

        private Matrix4f buildCurrentViewMatrix()
        {
            Matrix4f view = new Matrix4f();
            if(mapFocusMode)
            {
                view.lookAt(0.0f, 1.25f, 0.0f, 0.0f, 0.145f, 0.0f, 0.0f, 0.0f, -1.0f);
            }
            else
            {
                view.lookAt(0.0f, 2.4f, 2.8f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);
            }
            return view;
        }

        private Matrix4f buildCurrentProjectionMatrix()
        {
            Matrix4f projection = new Matrix4f();
            projection.perspective((float) Math.toRadians(mapFocusMode ? 50.0f : 45.0f),
                                   (float)swapChainExtent.width() / (float)swapChainExtent.height(),
                                   0.1f,
                                   10.0f);
            projection.m11(projection.m11() * -1);
            return projection;
        }

        private void cleanupSwapChain()
        {

            vkDestroyImageView(device, colorImageView, null);
            vkDestroyImage(device, colorImage, null);
            vkFreeMemory(device, colorImageMemory, null);

            vkDestroyImageView(device, depthImageView, null);
            vkDestroyImage(device, depthImage, null);
            vkFreeMemory(device, depthImageMemory, null);

            uniformBuffers.forEach(ubo -> vkDestroyBuffer(device, ubo, null));
            uniformBuffersMemory.forEach(uboMemory -> vkFreeMemory(device, uboMemory, null));

            vkDestroyDescriptorPool(device, descriptorPool, null);

            swapChainFramebuffers.forEach(framebuffer -> vkDestroyFramebuffer(device, framebuffer, null));

            try(MemoryStack stack = stackPush())
            {
                vkFreeCommandBuffers(device, commandPool, asPointerBuffer(stack, commandBuffers));
            }

            vkDestroyPipeline(device, graphicsPipeline, null);

            vkDestroyPipelineLayout(device, pipelineLayout, null);

            vkDestroyRenderPass(device, renderPass, null);

            swapChainImageViews.forEach(imageView -> vkDestroyImageView(device, imageView, null));

            vkDestroySwapchainKHR(device, swapChain, null);
        }

        private void cleanup()
        {

            cleanupSwapChain();

            vkDestroySampler(device, textureSampler, null);
            vkDestroyImageView(device, mapTextureImageView, null);
            vkDestroyImage(device, mapTextureImage, null);
            vkFreeMemory(device, mapTextureImageMemory, null);

            vkDestroyImageView(device, woodTextureImageView, null);
            vkDestroyImage(device, woodTextureImage, null);
            vkFreeMemory(device, woodTextureImageMemory, null);

            vkDestroyDescriptorSetLayout(device, descriptorSetLayout, null);

            vkDestroyBuffer(device, indexBuffer, null);
            vkFreeMemory(device, indexBufferMemory, null);

            vkDestroyBuffer(device, vertexBuffer, null);
            vkFreeMemory(device, vertexBufferMemory, null);

            inFlightFrames.forEach(frame ->
            {

                vkDestroySemaphore(device, frame.renderFinishedSemaphore(), null);
                vkDestroySemaphore(device, frame.imageAvailableSemaphore(), null);
                vkDestroyFence(device, frame.fence(), null);
            });
            inFlightFrames.clear();

            if(renderFinishedSemaphoresByImage != null)
            {
                renderFinishedSemaphoresByImage.forEach(semaphore -> vkDestroySemaphore(device, semaphore, null));
                renderFinishedSemaphoresByImage.clear();
            }

            vkDestroyCommandPool(device, commandPool, null);

            vkDestroyDevice(device, null);

            if(ENABLE_VALIDATION_LAYERS)
            {
                destroyDebugUtilsMessengerEXT(instance, debugMessenger, null);
            }

            vkDestroySurfaceKHR(instance, surface, null);

            vkDestroyInstance(instance, null);

            glfwDestroyWindow(window);

            glfwTerminate();
        }

        private void recreateSwapChain()
        {

            try(MemoryStack stack = stackPush())
            {

                IntBuffer width = stack.ints(0);
                IntBuffer height = stack.ints(0);

                while(width.get(0) == 0 && height.get(0) == 0)
                {
                    glfwGetFramebufferSize(window, width, height);
                    glfwWaitEvents();
                }
            }

            vkDeviceWaitIdle(device);

            cleanupSwapChain();

            createSwapChainObjects();
            recreateRenderFinishedSemaphores();
            imagesInFlight.clear();
        }

        private void createSwapChainObjects()
        {
            createSwapChain();
            createImageViews();
            createRenderPass();
            createGraphicsPipeline();
            createColorResources();
            createDepthResources();
            createFramebuffers();
            createUniformBuffers();
            createDescriptorPool();
            createDescriptorSets();
            createCommandBuffers();
        }

        private void createInstance()
        {

            if(ENABLE_VALIDATION_LAYERS && !checkValidationLayerSupport())
            {
                throw new RuntimeException("Validation requested but not supported");
            }

            try(MemoryStack stack = stackPush())
            {

                // Use calloc to initialize the structs with 0s. Otherwise, the program can crash due to random values

                VkApplicationInfo appInfo = VkApplicationInfo.calloc(stack);

                appInfo.sType(VK_STRUCTURE_TYPE_APPLICATION_INFO);
                appInfo.pApplicationName(stack.UTF8Safe("Hello Triangle"));
                appInfo.applicationVersion(VK_MAKE_VERSION(1, 0, 0));
                appInfo.pEngineName(stack.UTF8Safe("No Engine"));
                appInfo.engineVersion(VK_MAKE_VERSION(1, 0, 0));
                appInfo.apiVersion(VK_API_VERSION_1_0);

                VkInstanceCreateInfo createInfo = VkInstanceCreateInfo.calloc(stack);

                createInfo.sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO);
                createInfo.pApplicationInfo(appInfo);
                // enabledExtensionCount is implicitly set when you call ppEnabledExtensionNames
                createInfo.ppEnabledExtensionNames(getRequiredExtensions(stack));

                if(ENABLE_VALIDATION_LAYERS)
                {

                    createInfo.ppEnabledLayerNames(asPointerBuffer(stack, VALIDATION_LAYERS));

                    VkDebugUtilsMessengerCreateInfoEXT debugCreateInfo = VkDebugUtilsMessengerCreateInfoEXT.calloc(stack);
                    populateDebugMessengerCreateInfo(debugCreateInfo);
                    createInfo.pNext(debugCreateInfo.address());
                }

                // We need to retrieve the pointer of the created instance
                PointerBuffer instancePtr = stack.mallocPointer(1);

                if(vkCreateInstance(createInfo, null, instancePtr) != VK_SUCCESS)
                {
                    throw new RuntimeException("Failed to create instance");
                }

                instance = new VkInstance(instancePtr.get(0), createInfo);
            }
        }

        private void populateDebugMessengerCreateInfo(VkDebugUtilsMessengerCreateInfoEXT debugCreateInfo)
        {
            debugCreateInfo.sType(VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT);
            debugCreateInfo.messageSeverity(VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT);
            debugCreateInfo.messageType(VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT);
            debugCreateInfo.pfnUserCallback(jDemicApp::debugCallback);
        }

        private void setupDebugMessenger()
        {

            if(!ENABLE_VALIDATION_LAYERS)
            {
                return;
            }

            try(MemoryStack stack = stackPush())
            {

                VkDebugUtilsMessengerCreateInfoEXT createInfo = VkDebugUtilsMessengerCreateInfoEXT.calloc(stack);

                populateDebugMessengerCreateInfo(createInfo);

                LongBuffer pDebugMessenger = stack.longs(VK_NULL_HANDLE);

                if(createDebugUtilsMessengerEXT(instance, createInfo, null, pDebugMessenger) != VK_SUCCESS)
                {
                    throw new RuntimeException("Failed to set up debug messenger");
                }

                debugMessenger = pDebugMessenger.get(0);
            }
        }

        private void createSurface()
        {

            try(MemoryStack stack = stackPush())
            {

                LongBuffer pSurface = stack.longs(VK_NULL_HANDLE);

                if(glfwCreateWindowSurface(instance, window, null, pSurface) != VK_SUCCESS)
                {
                    throw new RuntimeException("Failed to create window surface");
                }

                surface = pSurface.get(0);
            }
        }

        private void pickPhysicalDevice()
        {

            try(MemoryStack stack = stackPush())
            {

                IntBuffer deviceCount = stack.ints(0);

                vkEnumeratePhysicalDevices(instance, deviceCount, null);

                if(deviceCount.get(0) == 0)
                {
                    throw new RuntimeException("Failed to find GPUs with Vulkan support");
                }

                PointerBuffer ppPhysicalDevices = stack.mallocPointer(deviceCount.get(0));

                vkEnumeratePhysicalDevices(instance, deviceCount, ppPhysicalDevices);

                for(int i = 0; i < ppPhysicalDevices.capacity(); i++)
                {

                    VkPhysicalDevice device = new VkPhysicalDevice(ppPhysicalDevices.get(i), instance);

                    if(isDeviceSuitable(device))
                    {
                        physicalDevice = device;
                        msaaSamples = getMaxUsableSampleCount();
                        return;
                    }
                }

                throw new RuntimeException("Failed to find a suitable GPU");
            }
        }

        private void createLogicalDevice()
        {

            try(MemoryStack stack = stackPush())
            {

                QueueFamilyIndices indices = findQueueFamilies(physicalDevice);

                int[] uniqueQueueFamilies = indices.unique();

                VkDeviceQueueCreateInfo.Buffer queueCreateInfos = VkDeviceQueueCreateInfo.calloc(uniqueQueueFamilies.length, stack);

                for(int i = 0; i < uniqueQueueFamilies.length; i++)
                {
                    VkDeviceQueueCreateInfo queueCreateInfo = queueCreateInfos.get(i);
                    queueCreateInfo.sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO);
                    queueCreateInfo.queueFamilyIndex(uniqueQueueFamilies[i]);
                    queueCreateInfo.pQueuePriorities(stack.floats(1.0f));
                }

                VkPhysicalDeviceFeatures deviceFeatures = VkPhysicalDeviceFeatures.calloc(stack);
                deviceFeatures.samplerAnisotropy(true);
                deviceFeatures.sampleRateShading(true); // Enable sample shading feature for the device

                VkDeviceCreateInfo createInfo = VkDeviceCreateInfo.calloc(stack);

                createInfo.sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO);
                createInfo.pQueueCreateInfos(queueCreateInfos);
                // queueCreateInfoCount is automatically set

                createInfo.pEnabledFeatures(deviceFeatures);

                createInfo.ppEnabledExtensionNames(asPointerBuffer(stack, DEVICE_EXTENSIONS));

                if(ENABLE_VALIDATION_LAYERS)
                {
                    createInfo.ppEnabledLayerNames(asPointerBuffer(stack, VALIDATION_LAYERS));
                }

                PointerBuffer pDevice = stack.pointers(VK_NULL_HANDLE);

                if(vkCreateDevice(physicalDevice, createInfo, null, pDevice) != VK_SUCCESS)
                {
                    throw new RuntimeException("Failed to create logical device");
                }

                device = new VkDevice(pDevice.get(0), physicalDevice, createInfo);

                PointerBuffer pQueue = stack.pointers(VK_NULL_HANDLE);

                vkGetDeviceQueue(device, indices.graphicsFamily, 0, pQueue);
                graphicsQueue = new VkQueue(pQueue.get(0), device);

                vkGetDeviceQueue(device, indices.presentFamily, 0, pQueue);
                presentQueue = new VkQueue(pQueue.get(0), device);
            }
        }

        private void createSwapChain()
        {

            try(MemoryStack stack = stackPush())
            {

                SwapChainSupportDetails swapChainSupport = querySwapChainSupport(physicalDevice, stack);

                VkSurfaceFormatKHR surfaceFormat = chooseSwapSurfaceFormat(swapChainSupport.formats);
                int presentMode = chooseSwapPresentMode(swapChainSupport.presentModes);
                VkExtent2D extent = chooseSwapExtent(stack, swapChainSupport.capabilities);

                IntBuffer imageCount = stack.ints(swapChainSupport.capabilities.minImageCount() + 1);

                if(swapChainSupport.capabilities.maxImageCount() > 0 && imageCount.get(0) > swapChainSupport.capabilities.maxImageCount())
                {
                    imageCount.put(0, swapChainSupport.capabilities.maxImageCount());
                }

                VkSwapchainCreateInfoKHR createInfo = VkSwapchainCreateInfoKHR.calloc(stack);

                createInfo.sType(VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR);
                createInfo.surface(surface);

                // Image settings
                createInfo.minImageCount(imageCount.get(0));
                createInfo.imageFormat(surfaceFormat.format());
                createInfo.imageColorSpace(surfaceFormat.colorSpace());
                createInfo.imageExtent(extent);
                createInfo.imageArrayLayers(1);
                createInfo.imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT);

                QueueFamilyIndices indices = findQueueFamilies(physicalDevice);

                if(!indices.graphicsFamily.equals(indices.presentFamily))
                {
                    createInfo.imageSharingMode(VK_SHARING_MODE_CONCURRENT);
                    createInfo.pQueueFamilyIndices(stack.ints(indices.graphicsFamily, indices.presentFamily));
                }
                else
                {
                    createInfo.imageSharingMode(VK_SHARING_MODE_EXCLUSIVE);
                }

                createInfo.preTransform(swapChainSupport.capabilities.currentTransform());
                createInfo.compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR);
                createInfo.presentMode(presentMode);
                createInfo.clipped(true);

                createInfo.oldSwapchain(VK_NULL_HANDLE);

                LongBuffer pSwapChain = stack.longs(VK_NULL_HANDLE);

                if(vkCreateSwapchainKHR(device, createInfo, null, pSwapChain) != VK_SUCCESS)
                {
                    throw new RuntimeException("Failed to create swap chain");
                }

                swapChain = pSwapChain.get(0);

                vkGetSwapchainImagesKHR(device, swapChain, imageCount, null);

                LongBuffer pSwapchainImages = stack.mallocLong(imageCount.get(0));

                vkGetSwapchainImagesKHR(device, swapChain, imageCount, pSwapchainImages);

                swapChainImages = new ArrayList<>(imageCount.get(0));

                for(int i = 0; i < pSwapchainImages.capacity(); i++)
                {
                    swapChainImages.add(pSwapchainImages.get(i));
                }

                swapChainImageFormat = surfaceFormat.format();
                swapChainExtent = VkExtent2D.create().set(extent);
            }
        }

        private void createImageViews()
        {

            swapChainImageViews = new ArrayList<>(swapChainImages.size());

            for(long swapChainImage : swapChainImages)
            {
                swapChainImageViews.add(createImageView(swapChainImage, swapChainImageFormat, VK_IMAGE_ASPECT_COLOR_BIT, 1));
            }
        }

        private void createRenderPass()
        {

            try(MemoryStack stack = stackPush())
            {

                VkAttachmentDescription.Buffer attachments = VkAttachmentDescription.calloc(3, stack);
                VkAttachmentReference.Buffer attachmentRefs = VkAttachmentReference.calloc(3, stack);

                // Color attachments

                // MSAA Image
                VkAttachmentDescription colorAttachment = attachments.get(0);
                colorAttachment.format(swapChainImageFormat);
                colorAttachment.samples(msaaSamples);
                colorAttachment.loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR);
                colorAttachment.storeOp(VK_ATTACHMENT_STORE_OP_STORE);
                colorAttachment.stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE);
                colorAttachment.stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE);
                colorAttachment.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
                colorAttachment.finalLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

                VkAttachmentReference colorAttachmentRef = attachmentRefs.get(0);
                colorAttachmentRef.attachment(0);
                colorAttachmentRef.layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

                // Present Image
                VkAttachmentDescription colorAttachmentResolve = attachments.get(2);
                colorAttachmentResolve.format(swapChainImageFormat);
                colorAttachmentResolve.samples(VK_SAMPLE_COUNT_1_BIT);
                colorAttachmentResolve.loadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE);
                colorAttachmentResolve.storeOp(VK_ATTACHMENT_STORE_OP_STORE);
                colorAttachmentResolve.stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE);
                colorAttachmentResolve.stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE);
                colorAttachmentResolve.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
                colorAttachmentResolve.finalLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);

                VkAttachmentReference colorAttachmentResolveRef = attachmentRefs.get(2);
                colorAttachmentResolveRef.attachment(2);
                colorAttachmentResolveRef.layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);


                // Depth-Stencil attachments

                VkAttachmentDescription depthAttachment = attachments.get(1);
                depthAttachment.format(findDepthFormat());
                depthAttachment.samples(msaaSamples);
                depthAttachment.loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR);
                depthAttachment.storeOp(VK_ATTACHMENT_STORE_OP_DONT_CARE);
                depthAttachment.stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE);
                depthAttachment.stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE);
                depthAttachment.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
                depthAttachment.finalLayout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);

                VkAttachmentReference depthAttachmentRef = attachmentRefs.get(1);
                depthAttachmentRef.attachment(1);
                depthAttachmentRef.layout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);

                VkSubpassDescription.Buffer subpass = VkSubpassDescription.calloc(1, stack);
                subpass.pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS);
                subpass.colorAttachmentCount(1);
                subpass.pColorAttachments(VkAttachmentReference.calloc(1, stack).put(0, colorAttachmentRef));
                subpass.pDepthStencilAttachment(depthAttachmentRef);
                subpass.pResolveAttachments(VkAttachmentReference.calloc(1, stack).put(0, colorAttachmentResolveRef));

                VkSubpassDependency.Buffer dependency = VkSubpassDependency.calloc(1, stack);
                dependency.srcSubpass(VK_SUBPASS_EXTERNAL);
                dependency.dstSubpass(0);
                dependency.srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT);
                dependency.srcAccessMask(0);
                dependency.dstStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT);
                dependency.dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_READ_BIT | VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);

                VkRenderPassCreateInfo renderPassInfo = VkRenderPassCreateInfo.calloc(stack);
                renderPassInfo.sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO);
                renderPassInfo.pAttachments(attachments);
                renderPassInfo.pSubpasses(subpass);
                renderPassInfo.pDependencies(dependency);

                LongBuffer pRenderPass = stack.mallocLong(1);

                if(vkCreateRenderPass(device, renderPassInfo, null, pRenderPass) != VK_SUCCESS)
                {
                    throw new RuntimeException("Failed to create render pass");
                }

                renderPass = pRenderPass.get(0);
            }
        }

        private void createDescriptorSetLayout()
        {

            try(MemoryStack stack = stackPush())
            {

                VkDescriptorSetLayoutBinding.Buffer bindings = VkDescriptorSetLayoutBinding.calloc(2, stack);

                VkDescriptorSetLayoutBinding uboLayoutBinding = bindings.get(0);
                uboLayoutBinding.binding(0);
                uboLayoutBinding.descriptorCount(1);
                uboLayoutBinding.descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
                uboLayoutBinding.pImmutableSamplers(null);
                uboLayoutBinding.stageFlags(VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT);

                VkDescriptorSetLayoutBinding samplerLayoutBinding = bindings.get(1);
                samplerLayoutBinding.binding(1);
                samplerLayoutBinding.descriptorCount(1);
                samplerLayoutBinding.descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
                samplerLayoutBinding.pImmutableSamplers(null);
                samplerLayoutBinding.stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT);

                VkDescriptorSetLayoutCreateInfo layoutInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack);
                layoutInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO);
                layoutInfo.pBindings(bindings);

                LongBuffer pDescriptorSetLayout = stack.mallocLong(1);

                if(vkCreateDescriptorSetLayout(device, layoutInfo, null, pDescriptorSetLayout) != VK_SUCCESS)
                {
                    throw new RuntimeException("Failed to create descriptor set layout");
                }
                descriptorSetLayout = pDescriptorSetLayout.get(0);
            }
        }

        private void createGraphicsPipeline()
        {

            try(MemoryStack stack = stackPush())
            {

                // Let's compile the GLSL shaders into SPIR-V at runtime using the shaderc library
                // Check ShaderSPIRVUtils class to see how it can be done
                SPIRV vertShaderSPIRV = compileShaderFile("shaders/table.vert", VERTEX_SHADER);
                SPIRV fragShaderSPIRV = compileShaderFile("shaders/table.frag", FRAGMENT_SHADER);

                long vertShaderModule = createShaderModule(vertShaderSPIRV.bytecode());
                long fragShaderModule = createShaderModule(fragShaderSPIRV.bytecode());

                ByteBuffer entryPoint = stack.UTF8("main");

                VkPipelineShaderStageCreateInfo.Buffer shaderStages = VkPipelineShaderStageCreateInfo.calloc(2, stack);

                VkPipelineShaderStageCreateInfo vertShaderStageInfo = shaderStages.get(0);

                vertShaderStageInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO);
                vertShaderStageInfo.stage(VK_SHADER_STAGE_VERTEX_BIT);
                vertShaderStageInfo.module(vertShaderModule);
                vertShaderStageInfo.pName(entryPoint);

                VkPipelineShaderStageCreateInfo fragShaderStageInfo = shaderStages.get(1);

                fragShaderStageInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO);
                fragShaderStageInfo.stage(VK_SHADER_STAGE_FRAGMENT_BIT);
                fragShaderStageInfo.module(fragShaderModule);
                fragShaderStageInfo.pName(entryPoint);

                // ===> VERTEX STAGE <===

                VkPipelineVertexInputStateCreateInfo vertexInputInfo = VkPipelineVertexInputStateCreateInfo.calloc(stack);
                vertexInputInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO);
                vertexInputInfo.pVertexBindingDescriptions(Vertex.getBindingDescription(stack));
                vertexInputInfo.pVertexAttributeDescriptions(Vertex.getAttributeDescriptions(stack));

                // ===> ASSEMBLY STAGE <===

                VkPipelineInputAssemblyStateCreateInfo inputAssembly = VkPipelineInputAssemblyStateCreateInfo.calloc(stack);
                inputAssembly.sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO);
                inputAssembly.topology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST);
                inputAssembly.primitiveRestartEnable(false);

                // ===> VIEWPORT & SCISSOR

                VkViewport.Buffer viewport = VkViewport.calloc(1, stack);
                viewport.x(0.0f);
                viewport.y(0.0f);
                viewport.width(swapChainExtent.width());
                viewport.height(swapChainExtent.height());
                viewport.minDepth(0.0f);
                viewport.maxDepth(1.0f);

                VkRect2D.Buffer scissor = VkRect2D.calloc(1, stack);
                scissor.offset(VkOffset2D.calloc(stack).set(0, 0));
                scissor.extent(swapChainExtent);

                VkPipelineViewportStateCreateInfo viewportState = VkPipelineViewportStateCreateInfo.calloc(stack);
                viewportState.sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO);
                viewportState.pViewports(viewport);
                viewportState.pScissors(scissor);

                // ===> RASTERIZATION STAGE <===

                VkPipelineRasterizationStateCreateInfo rasterizer = VkPipelineRasterizationStateCreateInfo.calloc(stack);
                rasterizer.sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO);
                rasterizer.depthClampEnable(false);
                rasterizer.rasterizerDiscardEnable(false);
                rasterizer.polygonMode(VK_POLYGON_MODE_FILL);
                rasterizer.lineWidth(1.0f);
                rasterizer.cullMode(VK_CULL_MODE_BACK_BIT);
                rasterizer.frontFace(VK_FRONT_FACE_COUNTER_CLOCKWISE);
                rasterizer.depthBiasEnable(false);

                // ===> MULTISAMPLING <===

                VkPipelineMultisampleStateCreateInfo multisampling = VkPipelineMultisampleStateCreateInfo.calloc(stack);
                multisampling.sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO);
                multisampling.sampleShadingEnable(true);
                multisampling.minSampleShading(0.2f); // Enable sample shading in the pipeline
                multisampling.rasterizationSamples(msaaSamples); // Min fraction for sample shading; closer to one is smoother

                VkPipelineDepthStencilStateCreateInfo depthStencil = VkPipelineDepthStencilStateCreateInfo.calloc(stack);
                depthStencil.sType(VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO);
                depthStencil.depthTestEnable(true);
                depthStencil.depthWriteEnable(true);
                depthStencil.depthCompareOp(VK_COMPARE_OP_LESS);
                depthStencil.depthBoundsTestEnable(false);
                depthStencil.minDepthBounds(0.0f); // Optional
                depthStencil.maxDepthBounds(1.0f); // Optional
                depthStencil.stencilTestEnable(false);

                // ===> COLOR BLENDING <===

                VkPipelineColorBlendAttachmentState.Buffer colorBlendAttachment = VkPipelineColorBlendAttachmentState.calloc(1, stack);
                colorBlendAttachment.colorWriteMask(VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT | VK_COLOR_COMPONENT_B_BIT | VK_COLOR_COMPONENT_A_BIT);
                colorBlendAttachment.blendEnable(false);

                VkPipelineColorBlendStateCreateInfo colorBlending = VkPipelineColorBlendStateCreateInfo.calloc(stack);
                colorBlending.sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO);
                colorBlending.logicOpEnable(false);
                colorBlending.logicOp(VK_LOGIC_OP_COPY);
                colorBlending.pAttachments(colorBlendAttachment);
                colorBlending.blendConstants(stack.floats(0.0f, 0.0f, 0.0f, 0.0f));

                // ===> PIPELINE LAYOUT CREATION <===

                VkPipelineLayoutCreateInfo pipelineLayoutInfo = VkPipelineLayoutCreateInfo.calloc(stack);
                pipelineLayoutInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO);
                pipelineLayoutInfo.pSetLayouts(stack.longs(descriptorSetLayout));

                LongBuffer pPipelineLayout = stack.longs(VK_NULL_HANDLE);

                if(vkCreatePipelineLayout(device, pipelineLayoutInfo, null, pPipelineLayout) != VK_SUCCESS)
                {
                    throw new RuntimeException("Failed to create pipeline layout");
                }

                pipelineLayout = pPipelineLayout.get(0);

                VkGraphicsPipelineCreateInfo.Buffer pipelineInfo = VkGraphicsPipelineCreateInfo.calloc(1, stack);
                pipelineInfo.sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO);
                pipelineInfo.pStages(shaderStages);
                pipelineInfo.pVertexInputState(vertexInputInfo);
                pipelineInfo.pInputAssemblyState(inputAssembly);
                pipelineInfo.pViewportState(viewportState);
                pipelineInfo.pRasterizationState(rasterizer);
                pipelineInfo.pMultisampleState(multisampling);
                pipelineInfo.pDepthStencilState(depthStencil);
                pipelineInfo.pColorBlendState(colorBlending);
                pipelineInfo.layout(pipelineLayout);
                pipelineInfo.renderPass(renderPass);
                pipelineInfo.subpass(0);
                pipelineInfo.basePipelineHandle(VK_NULL_HANDLE);
                pipelineInfo.basePipelineIndex(-1);

                LongBuffer pGraphicsPipeline = stack.mallocLong(1);

                if(vkCreateGraphicsPipelines(device, VK_NULL_HANDLE, pipelineInfo, null, pGraphicsPipeline) != VK_SUCCESS)
                {
                    throw new RuntimeException("Failed to create graphics pipeline");
                }

                graphicsPipeline = pGraphicsPipeline.get(0);

                // ===> RELEASE RESOURCES <===

                vkDestroyShaderModule(device, vertShaderModule, null);
                vkDestroyShaderModule(device, fragShaderModule, null);

                vertShaderSPIRV.free();
                fragShaderSPIRV.free();
            }
        }

        private void createFramebuffers()
        {

            swapChainFramebuffers = new ArrayList<>(swapChainImageViews.size());

            try(MemoryStack stack = stackPush())
            {

                LongBuffer attachments = stack.longs(colorImageView, depthImageView, VK_NULL_HANDLE);
                LongBuffer pFramebuffer = stack.mallocLong(1);

                // Lets allocate the create info struct once and just update the pAttachments field each iteration
                VkFramebufferCreateInfo framebufferInfo = VkFramebufferCreateInfo.calloc(stack);
                framebufferInfo.sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO);
                framebufferInfo.renderPass(renderPass);
                framebufferInfo.width(swapChainExtent.width());
                framebufferInfo.height(swapChainExtent.height());
                framebufferInfo.layers(1);

                for(long imageView : swapChainImageViews)
                {

                    attachments.put(2, imageView);

                    framebufferInfo.pAttachments(attachments);

                    if(vkCreateFramebuffer(device, framebufferInfo, null, pFramebuffer) != VK_SUCCESS)
                    {
                        throw new RuntimeException("Failed to create framebuffer");
                    }

                    swapChainFramebuffers.add(pFramebuffer.get(0));
                }
            }
        }

        private void createCommandPool()
        {

            try(MemoryStack stack = stackPush())
            {

                QueueFamilyIndices queueFamilyIndices = findQueueFamilies(physicalDevice);

                VkCommandPoolCreateInfo poolInfo = VkCommandPoolCreateInfo.calloc(stack);
                poolInfo.sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO);
                poolInfo.queueFamilyIndex(queueFamilyIndices.graphicsFamily);

                LongBuffer pCommandPool = stack.mallocLong(1);

                if (vkCreateCommandPool(device, poolInfo, null, pCommandPool) != VK_SUCCESS)
                {
                    throw new RuntimeException("Failed to create command pool");
                }

                commandPool = pCommandPool.get(0);
            }
        }

        private void createColorResources()
        {

            try(MemoryStack stack = stackPush())
            {

                LongBuffer pColorImage = stack.mallocLong(1);
                LongBuffer pColorImageMemory = stack.mallocLong(1);

                createImage(swapChainExtent.width(), swapChainExtent.height(),
                            1,
                            msaaSamples,
                            swapChainImageFormat,
                            VK_IMAGE_TILING_OPTIMAL,
                            VK_IMAGE_USAGE_TRANSIENT_ATTACHMENT_BIT | VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT,
                            VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT,
                            pColorImage,
                            pColorImageMemory);

                colorImage = pColorImage.get(0);
                colorImageMemory = pColorImageMemory.get(0);

                colorImageView = createImageView(colorImage, swapChainImageFormat, VK_IMAGE_ASPECT_COLOR_BIT, 1);

                transitionImageLayout(colorImage, swapChainImageFormat, VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL, 1);
            }
        }

        private void createDepthResources()
        {

            try(MemoryStack stack = stackPush())
            {

                int depthFormat = findDepthFormat();

                LongBuffer pDepthImage = stack.mallocLong(1);
                LongBuffer pDepthImageMemory = stack.mallocLong(1);

                createImage(
                    swapChainExtent.width(), swapChainExtent.height(),
                    1,
                    msaaSamples,
                    depthFormat,
                    VK_IMAGE_TILING_OPTIMAL,
                    VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT,
                    VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT,
                    pDepthImage,
                    pDepthImageMemory);

                depthImage = pDepthImage.get(0);
                depthImageMemory = pDepthImageMemory.get(0);

                depthImageView = createImageView(depthImage, depthFormat, VK_IMAGE_ASPECT_DEPTH_BIT, 1);

                // Explicitly transitioning the depth image
                transitionImageLayout(depthImage, depthFormat,
                                      VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL,
                                      1);

            }
        }

        private int findSupportedFormat(IntBuffer formatCandidates, int tiling, int features)
        {

            try(MemoryStack stack = stackPush())
            {

                VkFormatProperties props = VkFormatProperties.calloc(stack);

                for(int i = 0; i < formatCandidates.capacity(); ++i)
                {

                    int format = formatCandidates.get(i);

                    vkGetPhysicalDeviceFormatProperties(physicalDevice, format, props);

                    if(tiling == VK_IMAGE_TILING_LINEAR && (props.linearTilingFeatures() & features) == features)
                    {
                        return format;
                    }
                    else if(tiling == VK_IMAGE_TILING_OPTIMAL && (props.optimalTilingFeatures() & features) == features)
                    {
                        return format;
                    }

                }
            }

            throw new RuntimeException("Failed to find supported format");
        }


        private int findDepthFormat()
        {
            return findSupportedFormat(
                       stackGet().ints(VK_FORMAT_D32_SFLOAT, VK_FORMAT_D32_SFLOAT_S8_UINT, VK_FORMAT_D24_UNORM_S8_UINT),
                       VK_IMAGE_TILING_OPTIMAL,
                       VK_FORMAT_FEATURE_DEPTH_STENCIL_ATTACHMENT_BIT);
        }

        private boolean hasStencilComponent(int format)
        {
            return format == VK_FORMAT_D32_SFLOAT_S8_UINT || format == VK_FORMAT_D24_UNORM_S8_UINT;
        }

        private double log2(double n)
        {
            return Math.log(n) / Math.log(2);
        }

        private void createTextureImages()
        {
            TextureResource wood = createTextureImage("textures/woodtexture.png");
            woodMipLevels = wood.mipLevels;
            woodTextureImage = wood.image;
            woodTextureImageMemory = wood.memory;

            TextureResource map = createTextureImage(PANDEMIC_MAP_TEXTURE_PATH);
            mapMipLevels = map.mipLevels;
            mapTextureImage = map.image;
            mapTextureImageMemory = map.memory;
        }

        private TextureResource createTextureImage(String resourcePath)
        {

            try(MemoryStack stack = stackPush())
            {

                URI resourceUri = Objects.requireNonNull(
                                      Thread.currentThread().getContextClassLoader().getResource(resourcePath),
                                      "Missing texture resource: " + resourcePath).toURI();
                String filename = new java.io.File(resourceUri).getPath();

                LoadedTextureData textureData = loadTextureData(resourcePath, filename, stack);
                ByteBuffer pixels = textureData.pixels;

                long imageSize = (long) textureData.width * textureData.height * 4;
                int mipLevels = (int) Math.floor(log2(Math.max(textureData.width, textureData.height))) + 1;

                LongBuffer pStagingBuffer = stack.mallocLong(1);
                LongBuffer pStagingBufferMemory = stack.mallocLong(1);
                createBuffer(imageSize,
                             VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                             VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                             pStagingBuffer,
                             pStagingBufferMemory);

                PointerBuffer data = stack.mallocPointer(1);
                vkMapMemory(device, pStagingBufferMemory.get(0), 0, imageSize, 0, data);
                memcpy(data.getByteBuffer(0, (int) imageSize), pixels, imageSize);
                vkUnmapMemory(device, pStagingBufferMemory.get(0));

                if(textureData.stbOwned)
                {
                    stbi_image_free(pixels);
                }
                else
                {
                    memFree(pixels);
                }

                LongBuffer pTextureImage = stack.mallocLong(1);
                LongBuffer pTextureImageMemory = stack.mallocLong(1);
                createImage(textureData.width, textureData.height,
                            mipLevels,
                            VK_SAMPLE_COUNT_1_BIT, VK_FORMAT_R8G8B8A8_SRGB, VK_IMAGE_TILING_OPTIMAL,
                            VK_IMAGE_USAGE_TRANSFER_SRC_BIT | VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT,
                            VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT,
                            pTextureImage,
                            pTextureImageMemory);

                long textureImage = pTextureImage.get(0);
                long textureImageMemory = pTextureImageMemory.get(0);

                transitionImageLayout(textureImage,
                                      VK_FORMAT_R8G8B8A8_SRGB,
                                      VK_IMAGE_LAYOUT_UNDEFINED,
                                      VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                                      mipLevels);
                copyBufferToImage(pStagingBuffer.get(0), textureImage, textureData.width, textureData.height);
                generateMipmaps(textureImage, VK_FORMAT_R8G8B8A8_SRGB, textureData.width, textureData.height, mipLevels);

                vkDestroyBuffer(device, pStagingBuffer.get(0), null);
                vkFreeMemory(device, pStagingBufferMemory.get(0), null);

                return new TextureResource(textureImage, textureImageMemory, mipLevels);
            }
            catch (URISyntaxException e)
            {
                throw new RuntimeException("Failed to resolve texture resource path: " + resourcePath, e);
            }
        }

        private LoadedTextureData loadTextureData(String resourcePath, String filename, MemoryStack stack)
        {
            if(PANDEMIC_MAP_TEXTURE_PATH.equals(resourcePath))
            {
                return createAnnotatedPandemicMapTextureData(filename);
            }

            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);
            IntBuffer pChannels = stack.mallocInt(1);
            ByteBuffer pixels = stbi_load(filename, pWidth, pHeight, pChannels, STBI_rgb_alpha);

            if(pixels == null)
            {
                throw new RuntimeException("Failed to load texture image " + filename);
            }

            return new LoadedTextureData(pixels, pWidth.get(0), pHeight.get(0), true);
        }

        private LoadedTextureData createAnnotatedPandemicMapTextureData(String filename)
        {
            try
            {
                BufferedImage source = ImageIO.read(new File(filename));
                if(source == null)
                {
                    throw new IOException("Could not decode map image file");
                }

                pandemicMapBaseImage = source;
                mapTextureWidth = source.getWidth();
                mapTextureHeight = source.getHeight();
                var tempCity = new PandemicMapGraph();
                pandemicCities = tempCity.getCityList();
                alignedCityNodes = alignNodesToMapBackground(source, pandemicCities);

                BufferedImage annotated = createAnnotatedMapImage(-1);
                return new LoadedTextureData(toRgbaByteBuffer(annotated), annotated.getWidth(), annotated.getHeight(), false);
            }
            catch (IOException e)
            {
                throw new RuntimeException("Failed to annotate pandemic map texture: " + filename, e);
            }
        }

        private BufferedImage createAnnotatedMapImage(int hoveredIndex)
        {
            int w = pandemicMapBaseImage.getWidth();
            int h = pandemicMapBaseImage.getHeight();

            BufferedImage annotated = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = annotated.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int borderW = Math.max(6, (int)(w * MAP_BORDER_FRACTION));
            int borderH = Math.max(6, (int)(h * MAP_BORDER_FRACTION));

            g2d.setComposite(java.awt.AlphaComposite.Clear);
            g2d.fillRect(0, 0, w, h);
            g2d.setComposite(java.awt.AlphaComposite.SrcOver);

            g2d.drawImage(pandemicMapBaseImage, borderW, borderH, w - 2 * borderW, h - 2 * borderH, null);

            int fontSize = Math.max(12, w / 150);
            g2d.setFont(new Font("SansSerif", Font.BOLD, fontSize));
            FontMetrics metrics = g2d.getFontMetrics();

            int mapW = w - 2 * borderW;
            int mapH = h - 2 * borderH;

            for(int i = 0; i < alignedCityNodes.size(); i++)
            {
                AlignedCityNode alignedNode = alignedCityNodes.get(i);
                CityNode city = alignedNode.city;
                int nodeX = borderW + (int)((float) alignedNode.pixelX / w * mapW);
                int nodeY = borderH + (int)((float) alignedNode.pixelY / h * mapH);

                int nodeRadius = MAP_NODE_RADIUS_PX;
                Color nodeColor = colorForDisease(city.getNativeColor());
                g2d.setColor(nodeColor);
                g2d.fillOval(nodeX - nodeRadius, nodeY - nodeRadius, nodeRadius * 2, nodeRadius * 2);
                g2d.setColor(new Color(10, 10, 10, 220));
                g2d.drawOval(nodeX - nodeRadius, nodeY - nodeRadius, nodeRadius * 2, nodeRadius * 2);

                String label = city.getName();
                int textWidth = metrics.stringWidth(label);
                int labelX = Math.max(0, Math.min(nodeX - (textWidth / 2), w - textWidth));
                int labelY = nodeY - 10;
                if(labelY < metrics.getAscent())
                {
                    labelY = nodeY + metrics.getAscent() + 8;
                }

                drawCityLabel(g2d, metrics, label, labelX, labelY, w, h);
                g2d.drawString(label, labelX, labelY);
            }

            g2d.dispose();
            return annotated;
        }

        private List<AlignedCityNode> alignNodesToMapBackground(BufferedImage source, List<CityNode> cities)
        {
            List<AlignedCityNode> alignedNodes = new ArrayList<>(cities.size());

            for(CityNode city : cities)
            {
                float adjustedX = city.getRenderX() + MAP_NODE_ALIGNMENT_OFFSET_X;
                float adjustedY = city.getRenderY() + MAP_NODE_ALIGNMENT_OFFSET_Y;
                int expectedX = Math.round(adjustedX * source.getWidth());
                int expectedY = Math.round(adjustedY * source.getHeight());

                Vector2f snapped = snapNodePosition(source, city.getNativeColor(), expectedX, expectedY, 18);
                alignedNodes.add(new AlignedCityNode(city,
                                                     Math.round(snapped.x),
                                                     Math.round(snapped.y),
                                                     source.getWidth(),
                                                     source.getHeight()));
            }

            return alignedNodes;
        }

        private Vector2f snapNodePosition(BufferedImage image, CityNode.DiseaseColor diseaseColor, int expectedX, int expectedY, int radius)
        {
            int width = image.getWidth();
            int height = image.getHeight();
            int clampedExpectedX = Math.max(0, Math.min(expectedX, width - 1));
            int clampedExpectedY = Math.max(0, Math.min(expectedY, height - 1));

            float bestScore = Float.NEGATIVE_INFINITY;
            int bestX = clampedExpectedX;
            int bestY = clampedExpectedY;

            for(int y = Math.max(0, clampedExpectedY - radius); y <= Math.min(height - 1, clampedExpectedY + radius); y++)
            {
                for(int x = Math.max(0, clampedExpectedX - radius); x <= Math.min(width - 1, clampedExpectedX + radius); x++)
                {
                    int argb = image.getRGB(x, y);
                    int red = (argb >> 16) & 0xFF;
                    int green = (argb >> 8) & 0xFF;
                    int blue = argb & 0xFF;
                    float score = colorMatchScore(diseaseColor, red, green, blue);

                    int dx = x - clampedExpectedX;
                    int dy = y - clampedExpectedY;
                    float distancePenalty = (dx * dx + dy * dy) * 0.22f;
                    score -= distancePenalty;

                    if(score > bestScore)
                    {
                        bestScore = score;
                        bestX = x;
                        bestY = y;
                    }
                }
            }

            return new Vector2f(bestX, bestY);
        }

        private float colorMatchScore(CityNode.DiseaseColor diseaseColor, int red, int green, int blue)
        {
            return switch (diseaseColor)
            {
                case BLUE -> (blue * 2.2f) - (red * 0.8f) - (green * 0.5f);
                case YELLOW -> ((red + green) * 1.25f) - (blue * 1.4f);
                case RED -> (red * 2.3f) - (green * 0.9f) - (blue * 1.1f);
                case BLACK -> ((red + green + blue) * 0.95f) - Math.abs(red - green) - Math.abs(green - blue);
            };
        }

        private ByteBuffer toRgbaByteBuffer(BufferedImage image)
        {
            int width = image.getWidth();
            int height = image.getHeight();
            int[] argbPixels = new int[width * height];
            image.getRGB(0, 0, width, height, argbPixels, 0, width);

            ByteBuffer rgbaPixels = memAlloc(width * height * 4);
            for(int argbPixel : argbPixels)
            {
                rgbaPixels.put((byte) ((argbPixel >> 16) & 0xFF));
                rgbaPixels.put((byte) ((argbPixel >> 8) & 0xFF));
                rgbaPixels.put((byte) (argbPixel & 0xFF));
                rgbaPixels.put((byte) ((argbPixel >> 24) & 0xFF));
            }
            rgbaPixels.flip();
            return rgbaPixels;
        }

        private Color colorForDisease(CityNode.DiseaseColor diseaseColor)
        {
            return switch (diseaseColor)
            {
                case BLUE -> new Color(69, 130, 236);
                case YELLOW -> new Color(235, 197, 62);
                case BLACK -> new Color(64, 64, 64);
                case RED -> new Color(225, 69, 69);
            };
        }

        private void drawCityLabel(Graphics2D g2d, FontMetrics metrics, String label, int labelX, int labelY, int imageWidth, int imageHeight)
        {
            int paddingX = 3;
            int paddingY = 2;
            int textWidth = metrics.stringWidth(label);
            int textHeight = metrics.getAscent() + metrics.getDescent();
            int boxX = Math.max(0, labelX - paddingX);
            int boxY = Math.max(0, labelY - metrics.getAscent() - paddingY);
            int boxWidth = Math.min(imageWidth - boxX, textWidth + (paddingX));
            int boxHeight = Math.min(imageHeight - boxY, textHeight + (paddingY));

            g2d.setColor(new Color(12, 24, 46, 145));
            g2d.fill(new RoundRectangle2D.Float(boxX, boxY, boxWidth, boxHeight, 6, 6));
            g2d.setColor(new Color(122, 169, 216, 150));
            g2d.draw(new RoundRectangle2D.Float(boxX, boxY, boxWidth, boxHeight, 6, 6));
            g2d.setColor(new Color(15, 20, 28, 210));
            g2d.drawString(label, labelX + 1, labelY + 1);
            g2d.setColor(Color.WHITE);
        }

        private void generateMipmaps(long image, int imageFormat, int width, int height, int mipLevels)
        {

            try(MemoryStack stack = stackPush())
            {

                // Check if image format supports linear blitting
                VkFormatProperties formatProperties = VkFormatProperties.malloc(stack);
                vkGetPhysicalDeviceFormatProperties(physicalDevice, imageFormat, formatProperties);

                if((formatProperties.optimalTilingFeatures() & VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT) == 0)
                {
                    throw new RuntimeException("Texture image format does not support linear blitting");
                }

                VkCommandBuffer commandBuffer = beginSingleTimeCommands();

                VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc(1, stack);
                barrier.sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER);
                barrier.image(image);
                barrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
                barrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
                barrier.dstAccessMask(VK_QUEUE_FAMILY_IGNORED);
                barrier.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
                barrier.subresourceRange().baseArrayLayer(0);
                barrier.subresourceRange().layerCount(1);
                barrier.subresourceRange().levelCount(1);

                int mipWidth = width;
                int mipHeight = height;

                for(int i = 1; i < mipLevels; i++)
                {

                    barrier.subresourceRange().baseMipLevel(i - 1);
                    barrier.oldLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);
                    barrier.newLayout(VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL);
                    barrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
                    barrier.dstAccessMask(VK_ACCESS_TRANSFER_READ_BIT);

                    vkCmdPipelineBarrier(commandBuffer,
                                         VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT, 0,
                                         null,
                                         null,
                                         barrier);

                    VkImageBlit.Buffer blit = VkImageBlit.calloc(1, stack);
                    blit.srcOffsets(0).set(0, 0, 0);
                    blit.srcOffsets(1).set(mipWidth, mipHeight, 1);
                    blit.srcSubresource().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
                    blit.srcSubresource().mipLevel(i - 1);
                    blit.srcSubresource().baseArrayLayer(0);
                    blit.srcSubresource().layerCount(1);
                    blit.dstOffsets(0).set(0, 0, 0);
                    blit.dstOffsets(1).set(mipWidth > 1 ? mipWidth / 2 : 1, mipHeight > 1 ? mipHeight / 2 : 1, 1);
                    blit.dstSubresource().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
                    blit.dstSubresource().mipLevel(i);
                    blit.dstSubresource().baseArrayLayer(0);
                    blit.dstSubresource().layerCount(1);

                    vkCmdBlitImage(commandBuffer,
                                   image, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                                   image, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                                   blit,
                                   VK_FILTER_LINEAR);

                    barrier.oldLayout(VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL);
                    barrier.newLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
                    barrier.srcAccessMask(VK_ACCESS_TRANSFER_READ_BIT);
                    barrier.dstAccessMask(VK_ACCESS_SHADER_READ_BIT);

                    vkCmdPipelineBarrier(commandBuffer,
                                         VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT, 0,
                                         null,
                                         null,
                                         barrier);

                    if(mipWidth > 1)
                    {
                        mipWidth /= 2;
                    }

                    if(mipHeight > 1)
                    {
                        mipHeight /= 2;
                    }
                }

                barrier.subresourceRange().baseMipLevel(mipLevels - 1);
                barrier.oldLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);
                barrier.newLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
                barrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
                barrier.dstAccessMask(VK_ACCESS_SHADER_READ_BIT);

                vkCmdPipelineBarrier(commandBuffer,
                                     VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT, 0,
                                     null,
                                     null,
                                     barrier);

                endSingleTimeCommands(commandBuffer);
            }
        }

        private int getMaxUsableSampleCount()
        {

            try(MemoryStack stack = stackPush())
            {

                VkPhysicalDeviceProperties physicalDeviceProperties = VkPhysicalDeviceProperties.malloc(stack);
                vkGetPhysicalDeviceProperties(physicalDevice, physicalDeviceProperties);

                int sampleCountFlags = physicalDeviceProperties.limits().framebufferColorSampleCounts()
                                       & physicalDeviceProperties.limits().framebufferDepthSampleCounts();

                if((sampleCountFlags & VK_SAMPLE_COUNT_64_BIT) != 0)
                {
                    return VK_SAMPLE_COUNT_64_BIT;
                }
                if((sampleCountFlags & VK_SAMPLE_COUNT_32_BIT) != 0)
                {
                    return VK_SAMPLE_COUNT_32_BIT;
                }
                if((sampleCountFlags & VK_SAMPLE_COUNT_16_BIT) != 0)
                {
                    return VK_SAMPLE_COUNT_16_BIT;
                }
                if((sampleCountFlags & VK_SAMPLE_COUNT_8_BIT) != 0)
                {
                    return VK_SAMPLE_COUNT_8_BIT;
                }
                if((sampleCountFlags & VK_SAMPLE_COUNT_4_BIT) != 0)
                {
                    return VK_SAMPLE_COUNT_4_BIT;
                }
                if((sampleCountFlags & VK_SAMPLE_COUNT_2_BIT) != 0)
                {
                    return VK_SAMPLE_COUNT_2_BIT;
                }

                return VK_SAMPLE_COUNT_1_BIT;
            }
        }

        private void createTextureImageViews()
        {
            woodTextureImageView = createImageView(woodTextureImage, VK_FORMAT_R8G8B8A8_SRGB, VK_IMAGE_ASPECT_COLOR_BIT, woodMipLevels);
            mapTextureImageView = createImageView(mapTextureImage, VK_FORMAT_R8G8B8A8_SRGB, VK_IMAGE_ASPECT_COLOR_BIT, mapMipLevels);
        }

        private void createTextureSampler()
        {

            try(MemoryStack stack = stackPush())
            {

                VkSamplerCreateInfo samplerInfo = VkSamplerCreateInfo.calloc(stack);
                samplerInfo.sType(VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO);
                samplerInfo.magFilter(VK_FILTER_LINEAR);
                samplerInfo.minFilter(VK_FILTER_LINEAR);
                samplerInfo.addressModeU(VK_SAMPLER_ADDRESS_MODE_REPEAT);
                samplerInfo.addressModeV(VK_SAMPLER_ADDRESS_MODE_REPEAT);
                samplerInfo.addressModeW(VK_SAMPLER_ADDRESS_MODE_REPEAT);
                samplerInfo.anisotropyEnable(true);
                samplerInfo.maxAnisotropy(16.0f);
                samplerInfo.borderColor(VK_BORDER_COLOR_INT_OPAQUE_BLACK);
                samplerInfo.unnormalizedCoordinates(false);
                samplerInfo.compareEnable(false);
                samplerInfo.compareOp(VK_COMPARE_OP_ALWAYS);
                samplerInfo.mipmapMode(VK_SAMPLER_MIPMAP_MODE_LINEAR);
                samplerInfo.minLod(0); // Optional
                samplerInfo.maxLod((float) Math.max(woodMipLevels, mapMipLevels));
                samplerInfo.mipLodBias(0); // Optional

                LongBuffer pTextureSampler = stack.mallocLong(1);

                if(vkCreateSampler(device, samplerInfo, null, pTextureSampler) != VK_SUCCESS)
                {
                    throw new RuntimeException("Failed to create texture sampler");
                }

                textureSampler = pTextureSampler.get(0);
            }
        }

        private long createImageView(long image, int format, int aspectFlags, int mipLevels)
        {

            try(MemoryStack stack = stackPush())
            {

                VkImageViewCreateInfo viewInfo = VkImageViewCreateInfo.calloc(stack);
                viewInfo.sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO);
                viewInfo.image(image);
                viewInfo.viewType(VK_IMAGE_VIEW_TYPE_2D);
                viewInfo.format(format);
                viewInfo.subresourceRange().aspectMask(aspectFlags);
                viewInfo.subresourceRange().baseMipLevel(0);
                viewInfo.subresourceRange().levelCount(mipLevels);
                viewInfo.subresourceRange().baseArrayLayer(0);
                viewInfo.subresourceRange().layerCount(1);

                LongBuffer pImageView = stack.mallocLong(1);

                if(vkCreateImageView(device, viewInfo, null, pImageView) != VK_SUCCESS)
                {
                    throw new RuntimeException("Failed to create texture image view");
                }

                return pImageView.get(0);
            }
        }

        private void createImage(int width, int height, int mipLevels, int numSamples, int format, int tiling, int usage, int memProperties,
                                 LongBuffer pTextureImage, LongBuffer pTextureImageMemory)
        {

            try(MemoryStack stack = stackPush())
            {

                VkImageCreateInfo imageInfo = VkImageCreateInfo.calloc(stack);
                imageInfo.sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO);
                imageInfo.imageType(VK_IMAGE_TYPE_2D);
                imageInfo.extent().width(width);
                imageInfo.extent().height(height);
                imageInfo.extent().depth(1);
                imageInfo.mipLevels(mipLevels);
                imageInfo.arrayLayers(1);
                imageInfo.format(format);
                imageInfo.tiling(tiling);
                imageInfo.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
                imageInfo.usage(usage);
                imageInfo.samples(numSamples);
                imageInfo.sharingMode(VK_SHARING_MODE_EXCLUSIVE);

                if(vkCreateImage(device, imageInfo, null, pTextureImage) != VK_SUCCESS)
                {
                    throw new RuntimeException("Failed to create image");
                }

                VkMemoryRequirements memRequirements = VkMemoryRequirements.malloc(stack);
                vkGetImageMemoryRequirements(device, pTextureImage.get(0), memRequirements);

                VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.calloc(stack);
                allocInfo.sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO);
                allocInfo.allocationSize(memRequirements.size());
                allocInfo.memoryTypeIndex(findMemoryType(stack, memRequirements.memoryTypeBits(), memProperties));

                if(vkAllocateMemory(device, allocInfo, null, pTextureImageMemory) != VK_SUCCESS)
                {
                    throw new RuntimeException("Failed to allocate image memory");
                }

                vkBindImageMemory(device, pTextureImage.get(0), pTextureImageMemory.get(0), 0);
            }
        }

        private void transitionImageLayout(long image, int format, int oldLayout, int newLayout, int mipLevels)
        {

            try(MemoryStack stack = stackPush())
            {

                VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc(1, stack);
                barrier.sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER);
                barrier.oldLayout(oldLayout);
                barrier.newLayout(newLayout);
                barrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
                barrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
                barrier.image(image);

                barrier.subresourceRange().baseMipLevel(0);
                barrier.subresourceRange().levelCount(mipLevels);
                barrier.subresourceRange().baseArrayLayer(0);
                barrier.subresourceRange().layerCount(1);

                if(newLayout == VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL)
                {

                    barrier.subresourceRange().aspectMask(VK_IMAGE_ASPECT_DEPTH_BIT);

                    if(hasStencilComponent(format))
                    {
                        barrier.subresourceRange().aspectMask(
                            barrier.subresourceRange().aspectMask() | VK_IMAGE_ASPECT_STENCIL_BIT);
                    }

                }
                else
                {
                    barrier.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
                }

                int sourceStage;
                int destinationStage;

                if(oldLayout == VK_IMAGE_LAYOUT_UNDEFINED && newLayout == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
                {

                    barrier.srcAccessMask(0);
                    barrier.dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);

                    sourceStage = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
                    destinationStage = VK_PIPELINE_STAGE_TRANSFER_BIT;

                }
                else if(oldLayout == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL && newLayout == VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
                {

                    barrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
                    barrier.dstAccessMask(VK_ACCESS_SHADER_READ_BIT);

                    sourceStage = VK_PIPELINE_STAGE_TRANSFER_BIT;
                    destinationStage = VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT;

                }
                else if(oldLayout == VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL && newLayout == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
                {

                    barrier.srcAccessMask(VK_ACCESS_SHADER_READ_BIT);
                    barrier.dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);

                    sourceStage = VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT;
                    destinationStage = VK_PIPELINE_STAGE_TRANSFER_BIT;

                }
                else if (oldLayout == VK_IMAGE_LAYOUT_UNDEFINED && newLayout == VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL)
                {

                    barrier.srcAccessMask(0);
                    barrier.dstAccessMask(VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_READ_BIT | VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT);

                    sourceStage = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
                    destinationStage = VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT;

                }
                else if(oldLayout == VK_IMAGE_LAYOUT_UNDEFINED && newLayout == VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
                {

                    barrier.srcAccessMask(0);
                    barrier.dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_READ_BIT | VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);

                    sourceStage = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
                    destinationStage = VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;

                }
                else
                {
                    throw new IllegalArgumentException("Unsupported layout transition");
                }

                VkCommandBuffer commandBuffer = beginSingleTimeCommands();

                vkCmdPipelineBarrier(commandBuffer,
                                     sourceStage, destinationStage,
                                     0,
                                     null,
                                     null,
                                     barrier);

                endSingleTimeCommands(commandBuffer);
            }
        }

        private void copyBufferToImage(long buffer, long image, int width, int height)
        {

            try(MemoryStack stack = stackPush())
            {

                VkCommandBuffer commandBuffer = beginSingleTimeCommands();

                VkBufferImageCopy.Buffer region = VkBufferImageCopy.calloc(1, stack);
                region.bufferOffset(0);
                region.bufferRowLength(0);   // Tightly packed
                region.bufferImageHeight(0);  // Tightly packed
                region.imageSubresource().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
                region.imageSubresource().mipLevel(0);
                region.imageSubresource().baseArrayLayer(0);
                region.imageSubresource().layerCount(1);
                region.imageOffset().set(0, 0, 0);
                region.imageExtent(VkExtent3D.calloc(stack).set(width, height, 1));

                vkCmdCopyBufferToImage(commandBuffer, buffer, image, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, region);

                endSingleTimeCommands(commandBuffer);
            }
        }

        private void memcpy(ByteBuffer dst, ByteBuffer src, long size)
        {
            src.limit((int)size);
            dst.put(src);
            src.limit(src.capacity()).rewind();
        }

        private void loadModel()
        {

            List<Vertex> generatedVertices = new ArrayList<>();
            List<Integer> generatedIndices = new ArrayList<>();

            Vector3f wood = new Vector3f(0.42f, 0.25f, 0.13f);
            Vector3f white = new Vector3f(1.0f, 1.0f, 1.0f);

            ////////////
            //
            //   Manual table creation for now..
            //
            ////////////

            // Table top (large flat board)
            appendBox(generatedVertices, generatedIndices,
                      -1.05f, 1.05f,
                      0.06f, 0.14f,
                      -0.70f, 0.70f,
                      wood, wood);

            // Four table legs
            float legTopY = 0.06f;
            float legBottomY = -0.90f;
            appendBox(generatedVertices, generatedIndices, -0.95f, -0.83f, legBottomY, legTopY, -0.60f, -0.48f, wood, wood);
            appendBox(generatedVertices, generatedIndices, 0.83f, 0.95f, legBottomY, legTopY, -0.60f, -0.48f, wood, wood);
            appendBox(generatedVertices, generatedIndices, -0.95f, -0.83f, legBottomY, legTopY, 0.48f, 0.60f, wood, wood);
            appendBox(generatedVertices, generatedIndices, 0.83f, 0.95f, legBottomY, legTopY, 0.48f, 0.60f, wood, wood);

            // Centered map panel on top of the table
            mapPanelFirstIndex = generatedIndices.size();
            addQuad(generatedVertices, generatedIndices,
                    new Vector3f(-1.00f, 0.145f, 0.52f),
                    new Vector3f(1.00f, 0.145f, 0.52f),
                    new Vector3f(1.00f, 0.145f, -0.52f),
                    new Vector3f(-1.00f, 0.145f, -0.52f),
                    white);
            mapPanelIndexCount = generatedIndices.size() - mapPanelFirstIndex;
            woodIndexCount = mapPanelFirstIndex;

            vertices = generatedVertices.toArray(new Vertex[0]);
            indices = new int[generatedIndices.size()];
            for(int i = 0; i < generatedIndices.size(); i++)
            {
                indices[i] = generatedIndices.get(i);
            }
        }

        private void appendBox(List<Vertex> targetVertices, List<Integer> targetIndices,
                               float minX, float maxX,
                               float minY, float maxY,
                               float minZ, float maxZ,
                               Vector3fc topColor, Vector3fc sideColor)
        {

            // Front face
            addQuad(targetVertices, targetIndices,
                    new Vector3f(minX, minY, maxZ), new Vector3f(maxX, minY, maxZ),
                    new Vector3f(maxX, maxY, maxZ), new Vector3f(minX, maxY, maxZ),
                    sideColor);

            // Back face
            addQuad(targetVertices, targetIndices,
                    new Vector3f(maxX, minY, minZ), new Vector3f(minX, minY, minZ),
                    new Vector3f(minX, maxY, minZ), new Vector3f(maxX, maxY, minZ),
                    sideColor);

            // Left face
            addQuad(targetVertices, targetIndices,
                    new Vector3f(minX, minY, minZ), new Vector3f(minX, minY, maxZ),
                    new Vector3f(minX, maxY, maxZ), new Vector3f(minX, maxY, minZ),
                    sideColor);

            // Right face
            addQuad(targetVertices, targetIndices,
                    new Vector3f(maxX, minY, maxZ), new Vector3f(maxX, minY, minZ),
                    new Vector3f(maxX, maxY, minZ), new Vector3f(maxX, maxY, maxZ),
                    sideColor);

            // Top face
            addQuad(targetVertices, targetIndices,
                    new Vector3f(minX, maxY, maxZ), new Vector3f(maxX, maxY, maxZ),
                    new Vector3f(maxX, maxY, minZ), new Vector3f(minX, maxY, minZ),
                    topColor);

            // Bottom face
            addQuad(targetVertices, targetIndices,
                    new Vector3f(minX, minY, minZ), new Vector3f(maxX, minY, minZ),
                    new Vector3f(maxX, minY, maxZ), new Vector3f(minX, minY, maxZ),
                    sideColor);
        }

        private void addQuad(List<Vertex> targetVertices, List<Integer> targetIndices,
                             Vector3fc p0, Vector3fc p1, Vector3fc p2, Vector3fc p3,
                             Vector3fc color)
        {

            int base = targetVertices.size();

            targetVertices.add(new Vertex(p0, color, new Vector2f(0.0f, 1.0f)));
            targetVertices.add(new Vertex(p1, color, new Vector2f(1.0f, 1.0f)));
            targetVertices.add(new Vertex(p2, color, new Vector2f(1.0f, 0.0f)));
            targetVertices.add(new Vertex(p3, color, new Vector2f(0.0f, 0.0f)));

            targetIndices.add(base);
            targetIndices.add(base + 1);
            targetIndices.add(base + 2);
            targetIndices.add(base + 2);
            targetIndices.add(base + 3);
            targetIndices.add(base);
        }

        private void createVertexBuffer()
        {

            try(MemoryStack stack = stackPush())
            {

                long bufferSize = Vertex.SIZEOF * vertices.length;

                LongBuffer pBuffer = stack.mallocLong(1);
                LongBuffer pBufferMemory = stack.mallocLong(1);
                createBuffer(bufferSize,
                             VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                             VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                             pBuffer,
                             pBufferMemory);

                long stagingBuffer = pBuffer.get(0);
                long stagingBufferMemory = pBufferMemory.get(0);

                PointerBuffer data = stack.mallocPointer(1);

                vkMapMemory(device, stagingBufferMemory, 0, bufferSize, 0, data);
                {
                    memcpy(data.getByteBuffer(0, (int) bufferSize), vertices);
                }
                vkUnmapMemory(device, stagingBufferMemory);

                createBuffer(bufferSize,
                             VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_VERTEX_BUFFER_BIT,
                             VK_MEMORY_HEAP_DEVICE_LOCAL_BIT,
                             pBuffer,
                             pBufferMemory);

                vertexBuffer = pBuffer.get(0);
                vertexBufferMemory = pBufferMemory.get(0);

                copyBuffer(stagingBuffer, vertexBuffer, bufferSize);

                vkDestroyBuffer(device, stagingBuffer, null);
                vkFreeMemory(device, stagingBufferMemory, null);
            }
        }

        private void createIndexBuffer()
        {

            try(MemoryStack stack = stackPush())
            {

                long bufferSize = Integer.BYTES * indices.length;

                LongBuffer pBuffer = stack.mallocLong(1);
                LongBuffer pBufferMemory = stack.mallocLong(1);
                createBuffer(bufferSize,
                             VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                             VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                             pBuffer,
                             pBufferMemory);

                long stagingBuffer = pBuffer.get(0);
                long stagingBufferMemory = pBufferMemory.get(0);

                PointerBuffer data = stack.mallocPointer(1);

                vkMapMemory(device, stagingBufferMemory, 0, bufferSize, 0, data);
                {
                    memcpy(data.getByteBuffer(0, (int) bufferSize), indices);
                }
                vkUnmapMemory(device, stagingBufferMemory);

                createBuffer(bufferSize,
                             VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_INDEX_BUFFER_BIT,
                             VK_MEMORY_HEAP_DEVICE_LOCAL_BIT,
                             pBuffer,
                             pBufferMemory);

                indexBuffer = pBuffer.get(0);
                indexBufferMemory = pBufferMemory.get(0);

                copyBuffer(stagingBuffer, indexBuffer, bufferSize);

                vkDestroyBuffer(device, stagingBuffer, null);
                vkFreeMemory(device, stagingBufferMemory, null);
            }
        }

        private void createUniformBuffers()
        {

            try(MemoryStack stack = stackPush())
            {

                uniformBuffers = new ArrayList<>(swapChainImages.size());
                uniformBuffersMemory = new ArrayList<>(swapChainImages.size());

                LongBuffer pBuffer = stack.mallocLong(1);
                LongBuffer pBufferMemory = stack.mallocLong(1);

                for(int i = 0; i < swapChainImages.size(); i++)
                {
                    createBuffer(UniformBufferObject.SIZEOF,
                                 VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT,
                                 VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                                 pBuffer,
                                 pBufferMemory);

                    uniformBuffers.add(pBuffer.get(0));
                    uniformBuffersMemory.add(pBufferMemory.get(0));
                }

            }
        }


        private void createDescriptorPool()
        {

            try(MemoryStack stack = stackPush())
            {

                VkDescriptorPoolSize.Buffer poolSizes = VkDescriptorPoolSize.calloc(2, stack);

                VkDescriptorPoolSize uniformBufferPoolSize = poolSizes.get(0);
                uniformBufferPoolSize.type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
                uniformBufferPoolSize.descriptorCount(swapChainImages.size() * 2);

                VkDescriptorPoolSize textureSamplerPoolSize = poolSizes.get(1);
                textureSamplerPoolSize.type(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
                textureSamplerPoolSize.descriptorCount(swapChainImages.size() * 2);

                VkDescriptorPoolCreateInfo poolInfo = VkDescriptorPoolCreateInfo.calloc(stack);
                poolInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO);
                poolInfo.pPoolSizes(poolSizes);
                poolInfo.maxSets(swapChainImages.size() * 2);

                LongBuffer pDescriptorPool = stack.mallocLong(1);

                if(vkCreateDescriptorPool(device, poolInfo, null, pDescriptorPool) != VK_SUCCESS)
                {
                    throw new RuntimeException("Failed to create descriptor pool");
                }

                descriptorPool = pDescriptorPool.get(0);
            }
        }

        private void createDescriptorSets()
        {

            try(MemoryStack stack = stackPush())
            {

                LongBuffer layouts = stack.mallocLong(swapChainImages.size());
                for(int i = 0; i < layouts.capacity(); i++)
                {
                    layouts.put(i, descriptorSetLayout);
                }

                VkDescriptorSetAllocateInfo allocInfo = VkDescriptorSetAllocateInfo.calloc(stack);
                allocInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO);
                allocInfo.descriptorPool(descriptorPool);
                allocInfo.pSetLayouts(layouts);

                descriptorSetsWood = allocateAndWriteDescriptorSets(stack, allocInfo, woodTextureImageView);
                descriptorSetsMap = allocateAndWriteDescriptorSets(stack, allocInfo, mapTextureImageView);
            }
        }

        private List<Long> allocateAndWriteDescriptorSets(MemoryStack stack, VkDescriptorSetAllocateInfo allocInfo, long imageView)
        {

            LongBuffer pDescriptorSets = stack.mallocLong(swapChainImages.size());

            if(vkAllocateDescriptorSets(device, allocInfo, pDescriptorSets) != VK_SUCCESS)
            {
                throw new RuntimeException("Failed to allocate descriptor sets");
            }

            List<Long> descriptorSets = new ArrayList<>(pDescriptorSets.capacity());

            VkDescriptorBufferInfo.Buffer bufferInfo = VkDescriptorBufferInfo.calloc(1, stack);
            bufferInfo.offset(0);
            bufferInfo.range(UniformBufferObject.SIZEOF);

            VkDescriptorImageInfo.Buffer imageInfo = VkDescriptorImageInfo.calloc(1, stack);
            imageInfo.imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
            imageInfo.imageView(imageView);
            imageInfo.sampler(textureSampler);

            VkWriteDescriptorSet.Buffer descriptorWrites = VkWriteDescriptorSet.calloc(2, stack);

            VkWriteDescriptorSet uboDescriptorWrite = descriptorWrites.get(0);
            uboDescriptorWrite.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET);
            uboDescriptorWrite.dstBinding(0);
            uboDescriptorWrite.dstArrayElement(0);
            uboDescriptorWrite.descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
            uboDescriptorWrite.descriptorCount(1);
            uboDescriptorWrite.pBufferInfo(bufferInfo);

            VkWriteDescriptorSet samplerDescriptorWrite = descriptorWrites.get(1);
            samplerDescriptorWrite.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET);
            samplerDescriptorWrite.dstBinding(1);
            samplerDescriptorWrite.dstArrayElement(0);
            samplerDescriptorWrite.descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
            samplerDescriptorWrite.descriptorCount(1);
            samplerDescriptorWrite.pImageInfo(imageInfo);

            for(int i = 0; i < pDescriptorSets.capacity(); i++)
            {

                long descriptorSet = pDescriptorSets.get(i);

                bufferInfo.buffer(uniformBuffers.get(i));

                uboDescriptorWrite.dstSet(descriptorSet);
                samplerDescriptorWrite.dstSet(descriptorSet);

                vkUpdateDescriptorSets(device, descriptorWrites, null);

                descriptorSets.add(descriptorSet);
            }
            return descriptorSets;
        }

        private void createBuffer(long size, int usage, int properties, LongBuffer pBuffer, LongBuffer pBufferMemory)
        {

            try(MemoryStack stack = stackPush())
            {

                VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.calloc(stack);
                bufferInfo.sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO);
                bufferInfo.size(size);
                bufferInfo.usage(usage);
                bufferInfo.sharingMode(VK_SHARING_MODE_EXCLUSIVE);

                if(vkCreateBuffer(device, bufferInfo, null, pBuffer) != VK_SUCCESS)
                {
                    throw new RuntimeException("Failed to create vertex buffer");
                }

                VkMemoryRequirements memRequirements = VkMemoryRequirements.malloc(stack);
                vkGetBufferMemoryRequirements(device, pBuffer.get(0), memRequirements);

                VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.calloc(stack);
                allocInfo.sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO);
                allocInfo.allocationSize(memRequirements.size());
                allocInfo.memoryTypeIndex(findMemoryType(stack, memRequirements.memoryTypeBits(), properties));

                if(vkAllocateMemory(device, allocInfo, null, pBufferMemory) != VK_SUCCESS)
                {
                    throw new RuntimeException("Failed to allocate vertex buffer memory");
                }

                vkBindBufferMemory(device, pBuffer.get(0), pBufferMemory.get(0), 0);
            }
        }

        private VkCommandBuffer beginSingleTimeCommands()
        {

            try(MemoryStack stack = stackPush())
            {

                VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.calloc(stack);
                allocInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
                allocInfo.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY);
                allocInfo.commandPool(commandPool);
                allocInfo.commandBufferCount(1);

                PointerBuffer pCommandBuffer = stack.mallocPointer(1);
                vkAllocateCommandBuffers(device, allocInfo, pCommandBuffer);
                VkCommandBuffer commandBuffer = new VkCommandBuffer(pCommandBuffer.get(0), device);

                VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack);
                beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
                beginInfo.flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);

                vkBeginCommandBuffer(commandBuffer, beginInfo);

                return commandBuffer;
            }
        }

        private void endSingleTimeCommands(VkCommandBuffer commandBuffer)
        {

            try(MemoryStack stack = stackPush())
            {

                vkEndCommandBuffer(commandBuffer);

                VkSubmitInfo.Buffer submitInfo = VkSubmitInfo.calloc(1, stack);
                submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);
                submitInfo.pCommandBuffers(stack.pointers(commandBuffer));

                vkQueueSubmit(graphicsQueue, submitInfo, VK_NULL_HANDLE);
                vkQueueWaitIdle(graphicsQueue);

                vkFreeCommandBuffers(device, commandPool, commandBuffer);
            }
        }

        private void copyBuffer(long srcBuffer, long dstBuffer, long size)
        {

            try(MemoryStack stack = stackPush())
            {

                VkCommandBuffer commandBuffer = beginSingleTimeCommands();

                VkBufferCopy.Buffer copyRegion = VkBufferCopy.calloc(1, stack);
                copyRegion.size(size);

                vkCmdCopyBuffer(commandBuffer, srcBuffer, dstBuffer, copyRegion);

                endSingleTimeCommands(commandBuffer);
            }
        }

        private void memcpy(ByteBuffer buffer, Vertex[] vertices)
        {
            for(Vertex vertex : vertices)
            {
                buffer.putFloat(vertex.pos.x());
                buffer.putFloat(vertex.pos.y());
                buffer.putFloat(vertex.pos.z());

                buffer.putFloat(vertex.color.x());
                buffer.putFloat(vertex.color.y());
                buffer.putFloat(vertex.color.z());

                buffer.putFloat(vertex.texCoords.x());
                buffer.putFloat(vertex.texCoords.y());
            }
        }

        private void memcpy(ByteBuffer buffer, int[] indices)
        {

            for(int index : indices)
            {
                buffer.putInt(index);
            }

            buffer.rewind();
        }

        private void memcpy(ByteBuffer buffer, UniformBufferObject ubo)
        {

            final int mat4Size = 16 * Float.BYTES;

            ubo.model.get(0, buffer);
            ubo.view.get(alignas(mat4Size, alignof(ubo.view)), buffer);
            ubo.proj.get(alignas(mat4Size * 2, alignof(ubo.view)), buffer);
            int offset = mat4Size * 3;
            buffer.putFloat(offset, ubo.time);
            buffer.putFloat(offset + 4, ubo.hoverU);
            buffer.putFloat(offset + 8, ubo.hoverV);
            buffer.putFloat(offset + 12, ubo.hoverRadiusU);
            buffer.putFloat(offset + 16, ubo.hoverRadiusV);
            buffer.putFloat(offset + 20, ubo.hoverColorR);
            buffer.putFloat(offset + 24, ubo.hoverColorG);
            buffer.putFloat(offset + 28, ubo.hoverColorB);
        }

        private int findMemoryType(MemoryStack stack, int typeFilter, int properties)
        {

            VkPhysicalDeviceMemoryProperties memProperties = VkPhysicalDeviceMemoryProperties.malloc(stack);
            vkGetPhysicalDeviceMemoryProperties(physicalDevice, memProperties);

            for(int i = 0; i < memProperties.memoryTypeCount(); i++)
            {
                if((typeFilter & (1 << i)) != 0 && (memProperties.memoryTypes(i).propertyFlags() & properties) == properties)
                {
                    return i;
                }
            }

            throw new RuntimeException("Failed to find suitable memory type");
        }

        private void createCommandBuffers()
        {

            final int commandBuffersCount = swapChainFramebuffers.size();

            commandBuffers = new ArrayList<>(commandBuffersCount);

            try(MemoryStack stack = stackPush())
            {

                VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.calloc(stack);
                allocInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
                allocInfo.commandPool(commandPool);
                allocInfo.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY);
                allocInfo.commandBufferCount(commandBuffersCount);

                PointerBuffer pCommandBuffers = stack.mallocPointer(commandBuffersCount);

                if(vkAllocateCommandBuffers(device, allocInfo, pCommandBuffers) != VK_SUCCESS)
                {
                    throw new RuntimeException("Failed to allocate command buffers");
                }

                for(int i = 0; i < commandBuffersCount; i++)
                {
                    commandBuffers.add(new VkCommandBuffer(pCommandBuffers.get(i), device));
                }

                VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack);
                beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);

                VkRenderPassBeginInfo renderPassInfo = VkRenderPassBeginInfo.calloc(stack);
                renderPassInfo.sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO);

                renderPassInfo.renderPass(renderPass);

                VkRect2D renderArea = VkRect2D.calloc(stack);
                renderArea.offset(VkOffset2D.calloc(stack).set(0, 0));
                renderArea.extent(swapChainExtent);
                renderPassInfo.renderArea(renderArea);

                VkClearValue.Buffer clearValues = VkClearValue.calloc(2, stack);
                clearValues.get(0).color().float32(stack.floats(0.0f, 0.0f, 0.0f, 1.0f));
                clearValues.get(1).depthStencil().set(1.0f, 0);

                renderPassInfo.pClearValues(clearValues);

                for(int i = 0; i < commandBuffersCount; i++)
                {

                    VkCommandBuffer commandBuffer = commandBuffers.get(i);

                    if(vkBeginCommandBuffer(commandBuffer, beginInfo) != VK_SUCCESS)
                    {
                        throw new RuntimeException("Failed to begin recording command buffer");
                    }

                    renderPassInfo.framebuffer(swapChainFramebuffers.get(i));


                    vkCmdBeginRenderPass(commandBuffer, renderPassInfo, VK_SUBPASS_CONTENTS_INLINE);
                    {
                        vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, graphicsPipeline);

                        LongBuffer vertexBuffers = stack.longs(vertexBuffer);
                        LongBuffer offsets = stack.longs(0);
                        vkCmdBindVertexBuffers(commandBuffer, 0, vertexBuffers, offsets);

                        vkCmdBindIndexBuffer(commandBuffer, indexBuffer, 0, VK_INDEX_TYPE_UINT32);

                        vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS,
                                                pipelineLayout, 0, stack.longs(descriptorSetsWood.get(i)), null);

                        vkCmdDrawIndexed(commandBuffer, woodIndexCount, 1, 0, 0, 0);

                        vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS,
                                                pipelineLayout, 0, stack.longs(descriptorSetsMap.get(i)), null);

                        vkCmdDrawIndexed(commandBuffer, mapPanelIndexCount, 1, mapPanelFirstIndex, 0, 0);
                    }
                    vkCmdEndRenderPass(commandBuffer);


                    if(vkEndCommandBuffer(commandBuffer) != VK_SUCCESS)
                    {
                        throw new RuntimeException("Failed to record command buffer");
                    }

                }

            }
        }

        private void createSyncObjects()
        {

            inFlightFrames = new ArrayList<>(MAX_FRAMES_IN_FLIGHT);
            imagesInFlight = new HashMap<>(swapChainImages.size());

            try(MemoryStack stack = stackPush())
            {

                VkSemaphoreCreateInfo semaphoreInfo = VkSemaphoreCreateInfo.calloc(stack);
                semaphoreInfo.sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);

                VkFenceCreateInfo fenceInfo = VkFenceCreateInfo.calloc(stack);
                fenceInfo.sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO);
                fenceInfo.flags(VK_FENCE_CREATE_SIGNALED_BIT);

                LongBuffer pImageAvailableSemaphore = stack.mallocLong(1);
                LongBuffer pRenderFinishedSemaphore = stack.mallocLong(1);
                LongBuffer pFence = stack.mallocLong(1);

                for(int i = 0; i < MAX_FRAMES_IN_FLIGHT; i++)
                {

                    if(vkCreateSemaphore(device, semaphoreInfo, null, pImageAvailableSemaphore) != VK_SUCCESS
                            || vkCreateSemaphore(device, semaphoreInfo, null, pRenderFinishedSemaphore) != VK_SUCCESS
                            || vkCreateFence(device, fenceInfo, null, pFence) != VK_SUCCESS)
                    {

                        throw new RuntimeException("Failed to create synchronization objects for the frame " + i);
                    }

                    inFlightFrames.add(new Frame(pImageAvailableSemaphore.get(0), pRenderFinishedSemaphore.get(0), pFence.get(0)));
                }

            }

            recreateRenderFinishedSemaphores();
        }

        private void recreateRenderFinishedSemaphores()
        {

            if(renderFinishedSemaphoresByImage != null)
            {
                renderFinishedSemaphoresByImage.forEach(semaphore -> vkDestroySemaphore(device, semaphore, null));
            }

            renderFinishedSemaphoresByImage = new ArrayList<>(swapChainImages.size());

            try(MemoryStack stack = stackPush())
            {

                VkSemaphoreCreateInfo semaphoreInfo = VkSemaphoreCreateInfo.calloc(stack);
                semaphoreInfo.sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);

                LongBuffer pRenderFinishedSemaphore = stack.mallocLong(1);

                for(int i = 0; i < swapChainImages.size(); i++)
                {
                    if(vkCreateSemaphore(device, semaphoreInfo, null, pRenderFinishedSemaphore) != VK_SUCCESS)
                    {
                        throw new RuntimeException("Failed to create render-finished semaphore for swapchain image " + i);
                    }
                    renderFinishedSemaphoresByImage.add(pRenderFinishedSemaphore.get(0));
                }
            }
        }

        private void updateUniformBuffer(int currentImage)
        {

            try(MemoryStack stack = stackPush())
            {

                UniformBufferObject ubo = new UniformBufferObject();

                ubo.model.identity();
                ubo.view.set(buildCurrentViewMatrix());
                ubo.proj.set(buildCurrentProjectionMatrix());
                ubo.time = (float) glfwGetTime();

                if(hoveredCityIndex >= 0 && alignedCityNodes != null && hoveredCityIndex < alignedCityNodes.size())
                {
                    AlignedCityNode node = alignedCityNodes.get(hoveredCityIndex);
                    ubo.hoverU = MAP_BORDER_FRACTION + node.mapU * (1.0f - 2.0f * MAP_BORDER_FRACTION);
                    ubo.hoverV = MAP_BORDER_FRACTION + node.mapV * (1.0f - 2.0f * MAP_BORDER_FRACTION);
                    ubo.hoverRadiusU = (float) MAP_NODE_HOVER_RADIUS_PX / (float) mapTextureWidth;
                    ubo.hoverRadiusV = (float) MAP_NODE_HOVER_RADIUS_PX / (float) mapTextureHeight;
                    Color c = colorForDisease(node.city.getNativeColor());
                    ubo.hoverColorR = c.getRed() / 255.0f;
                    ubo.hoverColorG = c.getGreen() / 255.0f;
                    ubo.hoverColorB = c.getBlue() / 255.0f;
                }

                PointerBuffer data = stack.mallocPointer(1);
                vkMapMemory(device, uniformBuffersMemory.get(currentImage), 0, UniformBufferObject.SIZEOF, 0, data);
                {
                    memcpy(data.getByteBuffer(0, UniformBufferObject.SIZEOF), ubo);
                }
                vkUnmapMemory(device, uniformBuffersMemory.get(currentImage));
            }
        }

        private void drawFrame()
        {

            try(MemoryStack stack = stackPush())
            {

                Frame thisFrame = inFlightFrames.get(currentFrame);

                vkWaitForFences(device, thisFrame.pFence(), true, UINT64_MAX);

                IntBuffer pImageIndex = stack.mallocInt(1);

                int vkResult = vkAcquireNextImageKHR(device, swapChain, UINT64_MAX,
                                                     thisFrame.imageAvailableSemaphore(), VK_NULL_HANDLE, pImageIndex);

                if(vkResult == VK_ERROR_OUT_OF_DATE_KHR)
                {
                    recreateSwapChain();
                    return;
                }
                else if(vkResult != VK_SUCCESS && vkResult != VK_SUBOPTIMAL_KHR)
                {
                    throw new RuntimeException("Cannot get image");
                }

                final int imageIndex = pImageIndex.get(0);
                final long renderFinishedSemaphore = renderFinishedSemaphoresByImage.get(imageIndex);

                updateUniformBuffer(imageIndex);

                if(imagesInFlight.containsKey(imageIndex))
                {
                    vkWaitForFences(device, imagesInFlight.get(imageIndex).fence(), true, UINT64_MAX);
                }

                imagesInFlight.put(imageIndex, thisFrame);

                VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack);
                submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);

                submitInfo.waitSemaphoreCount(1);
                submitInfo.pWaitSemaphores(thisFrame.pImageAvailableSemaphore());
                submitInfo.pWaitDstStageMask(stack.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT));

                submitInfo.pSignalSemaphores(stack.longs(renderFinishedSemaphore));

                submitInfo.pCommandBuffers(stack.pointers(commandBuffers.get(imageIndex)));

                vkResetFences(device, thisFrame.pFence());

                if((vkResult = vkQueueSubmit(graphicsQueue, submitInfo, thisFrame.fence())) != VK_SUCCESS)
                {
                    vkResetFences(device, thisFrame.pFence());
                    throw new RuntimeException("Failed to submit draw command buffer: " + vkResult);
                }

                VkPresentInfoKHR presentInfo = VkPresentInfoKHR.calloc(stack);
                presentInfo.sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR);

                presentInfo.pWaitSemaphores(stack.longs(renderFinishedSemaphore));

                presentInfo.swapchainCount(1);
                presentInfo.pSwapchains(stack.longs(swapChain));

                presentInfo.pImageIndices(pImageIndex);

                vkResult = vkQueuePresentKHR(presentQueue, presentInfo);

                if(vkResult == VK_ERROR_OUT_OF_DATE_KHR || vkResult == VK_SUBOPTIMAL_KHR || framebufferResize)
                {
                    framebufferResize = false;
                    recreateSwapChain();
                }
                else if(vkResult != VK_SUCCESS)
                {
                    throw new RuntimeException("Failed to present swap chain image");
                }

                currentFrame = (currentFrame + 1) % MAX_FRAMES_IN_FLIGHT;
            }
        }

        private long createShaderModule(ByteBuffer spirvCode)
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

        private VkSurfaceFormatKHR chooseSwapSurfaceFormat(VkSurfaceFormatKHR.Buffer availableFormats)
        {
            return availableFormats.stream()
                   .filter(availableFormat -> availableFormat.format() == VK_FORMAT_B8G8R8_SRGB)
                   .filter(availableFormat -> availableFormat.colorSpace() == VK_COLOR_SPACE_SRGB_NONLINEAR_KHR)
                   .findAny()
                   .orElse(availableFormats.get(0));
        }

        private int chooseSwapPresentMode(IntBuffer availablePresentModes)
        {

            for(int i = 0; i < availablePresentModes.capacity(); i++)
            {
                if(availablePresentModes.get(i) == VK_PRESENT_MODE_MAILBOX_KHR)
                {
                    return availablePresentModes.get(i);
                }
            }

            return VK_PRESENT_MODE_FIFO_KHR;
        }

        private VkExtent2D chooseSwapExtent(MemoryStack stack, VkSurfaceCapabilitiesKHR capabilities)
        {

            if(capabilities.currentExtent().width() != UINT32_MAX)
            {
                return capabilities.currentExtent();
            }

            IntBuffer width = stackGet().ints(0);
            IntBuffer height = stackGet().ints(0);

            glfwGetFramebufferSize(window, width, height);

            VkExtent2D actualExtent = VkExtent2D.malloc(stack).set(width.get(0), height.get(0));

            VkExtent2D minExtent = capabilities.minImageExtent();
            VkExtent2D maxExtent = capabilities.maxImageExtent();

            actualExtent.width(clamp(minExtent.width(), maxExtent.width(), actualExtent.width()));
            actualExtent.height(clamp(minExtent.height(), maxExtent.height(), actualExtent.height()));

            return actualExtent;
        }

        private int clamp(int min, int max, int value)
        {
            return Math.max(min, Math.min(max, value));
        }

        private boolean isDeviceSuitable(VkPhysicalDevice device)
        {

            QueueFamilyIndices indices = findQueueFamilies(device);

            boolean extensionsSupported = checkDeviceExtensionSupport(device);
            boolean swapChainAdequate = false;
            boolean anisotropySupported = false;

            if(extensionsSupported)
            {
                try(MemoryStack stack = stackPush())
                {
                    SwapChainSupportDetails swapChainSupport = querySwapChainSupport(device, stack);
                    swapChainAdequate = swapChainSupport.formats.hasRemaining() && swapChainSupport.presentModes.hasRemaining();
                    VkPhysicalDeviceFeatures supportedFeatures = VkPhysicalDeviceFeatures.malloc(stack);
                    vkGetPhysicalDeviceFeatures(device, supportedFeatures);
                    anisotropySupported = supportedFeatures.samplerAnisotropy();
                }
            }

            return indices.isComplete() && extensionsSupported && swapChainAdequate && anisotropySupported;
        }

        private boolean checkDeviceExtensionSupport(VkPhysicalDevice device)
        {

            try(MemoryStack stack = stackPush())
            {

                IntBuffer extensionCount = stack.ints(0);

                vkEnumerateDeviceExtensionProperties(device, (String)null, extensionCount, null);

                VkExtensionProperties.Buffer availableExtensions = VkExtensionProperties.malloc(extensionCount.get(0), stack);

                vkEnumerateDeviceExtensionProperties(device, (String)null, extensionCount, availableExtensions);

                return availableExtensions.stream()
                       .map(VkExtensionProperties::extensionNameString)
                       .collect(toSet())
                       .containsAll(DEVICE_EXTENSIONS);
            }
        }

        private SwapChainSupportDetails querySwapChainSupport(VkPhysicalDevice device, MemoryStack stack)
        {

            SwapChainSupportDetails details = new SwapChainSupportDetails();

            details.capabilities = VkSurfaceCapabilitiesKHR.malloc(stack);
            vkGetPhysicalDeviceSurfaceCapabilitiesKHR(device, surface, details.capabilities);

            IntBuffer count = stack.ints(0);

            vkGetPhysicalDeviceSurfaceFormatsKHR(device, surface, count, null);

            if(count.get(0) != 0)
            {
                details.formats = VkSurfaceFormatKHR.malloc(count.get(0), stack);
                vkGetPhysicalDeviceSurfaceFormatsKHR(device, surface, count, details.formats);
            }

            vkGetPhysicalDeviceSurfacePresentModesKHR(device,surface, count, null);

            if(count.get(0) != 0)
            {
                details.presentModes = stack.mallocInt(count.get(0));
                vkGetPhysicalDeviceSurfacePresentModesKHR(device, surface, count, details.presentModes);
            }

            return details;
        }

        private QueueFamilyIndices findQueueFamilies(VkPhysicalDevice device)
        {

            QueueFamilyIndices indices = new QueueFamilyIndices();

            try(MemoryStack stack = stackPush())
            {

                IntBuffer queueFamilyCount = stack.ints(0);

                vkGetPhysicalDeviceQueueFamilyProperties(device, queueFamilyCount, null);

                VkQueueFamilyProperties.Buffer queueFamilies = VkQueueFamilyProperties.malloc(queueFamilyCount.get(0), stack);

                vkGetPhysicalDeviceQueueFamilyProperties(device, queueFamilyCount, queueFamilies);

                IntBuffer presentSupport = stack.ints(VK_FALSE);

                for(int i = 0; i < queueFamilies.capacity() || !indices.isComplete(); i++)
                {

                    if((queueFamilies.get(i).queueFlags() & VK_QUEUE_GRAPHICS_BIT) != 0)
                    {
                        indices.graphicsFamily = i;
                    }

                    vkGetPhysicalDeviceSurfaceSupportKHR(device, i, surface, presentSupport);

                    if(presentSupport.get(0) == VK_TRUE)
                    {
                        indices.presentFamily = i;
                    }
                }

                return indices;
            }
        }

        private PointerBuffer asPointerBuffer(MemoryStack stack, Collection<String> collection)
        {

            PointerBuffer buffer = stack.mallocPointer(collection.size());

            collection.stream()
            .map(stack::UTF8)
            .forEach(buffer::put);

            return buffer.rewind();
        }

        private PointerBuffer asPointerBuffer(MemoryStack stack, List<? extends Pointer> list)
        {

            PointerBuffer buffer = stack.mallocPointer(list.size());

            list.forEach(buffer::put);

            return buffer.rewind();
        }

        private PointerBuffer getRequiredExtensions(MemoryStack stack)
        {

            PointerBuffer glfwExtensions = glfwGetRequiredInstanceExtensions();

            if(ENABLE_VALIDATION_LAYERS)
            {

                PointerBuffer extensions = stack.mallocPointer(glfwExtensions.capacity() + 1);

                extensions.put(glfwExtensions);
                extensions.put(stack.UTF8(VK_EXT_DEBUG_UTILS_EXTENSION_NAME));

                // Rewind the buffer before returning it to reset its position back to 0
                return extensions.rewind();
            }

            return glfwExtensions;
        }

        private boolean checkValidationLayerSupport()
        {

            try(MemoryStack stack = stackPush())
            {

                IntBuffer layerCount = stack.ints(0);

                vkEnumerateInstanceLayerProperties(layerCount, null);

                VkLayerProperties.Buffer availableLayers = VkLayerProperties.malloc(layerCount.get(0), stack);

                vkEnumerateInstanceLayerProperties(layerCount, availableLayers);

                Set<String> availableLayerNames = availableLayers.stream()
                                                  .map(VkLayerProperties::layerNameString)
                                                  .collect(toSet());

                return availableLayerNames.containsAll(VALIDATION_LAYERS);
            }
        }

    }

    public static void main(String[] args)
    {

        jDemicApp app = new jDemicApp();

        app.run();
    }

}

public class main
{
    public static class EntryPoint
    {
        public static void main(String[] args)
        {
            jDemicEngine.main(args);
        }
    }
}
