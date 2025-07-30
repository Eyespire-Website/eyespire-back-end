const fs = require('fs');
const path = require('path');

// Production URLs
const BACKEND_URL = 'https://eyespire-back-end.onrender.com';
const FRONTEND_URL = 'https://eyespire.vercel.app';

// Files cần cập nhật
const filesToUpdate = [
    // Application properties
    'src/main/resources/application.properties',
    
    // Controllers
    'src/main/java/org/eyespire/eyespireapi/controller/AppointmentController.java',
    'src/main/java/org/eyespire/eyespireapi/controller/AuthController.java',
    'src/main/java/org/eyespire/eyespireapi/controller/DoctorController.java',
    'src/main/java/org/eyespire/eyespireapi/controller/MedicalRecordController.java',
    'src/main/java/org/eyespire/eyespireapi/controller/PaymentHistoryController.java',
    'src/main/java/org/eyespire/eyespireapi/controller/OrderPaymentController.java',
    'src/main/java/org/eyespire/eyespireapi/controller/OrderController.java',
    'src/main/java/org/eyespire/eyespireapi/controller/PayOSController.java',
    'src/main/java/org/eyespire/eyespireapi/controller/UserController.java',
    
    // Config files
    'src/main/java/org/eyespire/eyespireapi/config/AppConfig.java',
    'src/main/java/org/eyespire/eyespireapi/config/WebSocketConfig.java',
    
    // Services
    'src/main/java/org/eyespire/eyespireapi/service/EmailService.java',
    'src/main/java/org/eyespire/eyespireapi/service/OrderPaymentService.java',
    'src/main/java/org/eyespire/eyespireapi/service/UserService.java'
];

function updateFile(filePath) {
    const fullPath = path.join(__dirname, filePath);
    
    if (!fs.existsSync(fullPath)) {
        console.log(`❌ File not found: ${filePath}`);
        return;
    }
    
    try {
        let content = fs.readFileSync(fullPath, 'utf8');
        let updated = false;
        
        // Cập nhật frontend URLs
        const frontendReplacements = [
            ['http://localhost:3000', FRONTEND_URL],
            ['http://localhost:3001', FRONTEND_URL],
        ];
        
        // Cập nhật backend URLs
        const backendReplacements = [
            ['http://localhost:8080', BACKEND_URL],
        ];
        
        // Thực hiện thay thế
        [...frontendReplacements, ...backendReplacements].forEach(([oldUrl, newUrl]) => {
            if (content.includes(oldUrl)) {
                content = content.replaceAll(oldUrl, newUrl);
                updated = true;
            }
        });
        
        // Cập nhật Google OAuth redirect URI
        if (filePath.includes('application.properties')) {
            content = content.replace(
                /google\.redirect\.uri=http:\/\/localhost:3000\/auth\/google\/callback/g,
                `google.redirect.uri=${FRONTEND_URL}/auth/google/callback`
            );
            updated = true;
        }
        
        if (updated) {
            fs.writeFileSync(fullPath, content, 'utf8');
            console.log(`✅ Updated: ${filePath}`);
        } else {
            console.log(`⚪ No changes needed: ${filePath}`);
        }
        
    } catch (error) {
        console.error(`❌ Error updating ${filePath}:`, error.message);
    }
}

function main() {
    console.log('🚀 Updating backend URLs for production deployment...\n');
    console.log(`Backend URL: ${BACKEND_URL}`);
    console.log(`Frontend URL: ${FRONTEND_URL}\n`);
    
    filesToUpdate.forEach(updateFile);
    
    console.log('\n✅ Backend URL update completed!');
    console.log('\n📝 Next steps:');
    console.log('1. Commit and push changes to trigger Render redeploy');
    console.log('2. Update frontend URLs to use production backend');
    console.log('3. Test all functionality after deployment');
    console.log('\n🔗 Production URLs:');
    console.log(`Frontend: ${FRONTEND_URL}`);
    console.log(`Backend: ${BACKEND_URL}`);
}

main();
