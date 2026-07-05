const fs = require('fs');
const lines = fs.readFileSync('C:/Users/rikiw/.gemini/antigravity-ide/brain/668091c2-1fb0-4f73-93cf-b07fd3c358f8/.system_generated/logs/transcript.jsonl', 'utf-8').split('\n');
fs.writeFileSync('original_dashboard_xml.txt', '');
let inViewFile = false;
let foundFile = false;
let content = [];
for (let line of lines) {
    if (!line) continue;
    try {
        const obj = JSON.parse(line);
        if (obj.type === 'PLANNER_RESPONSE' && obj.tool_calls) {
            for (let tc of obj.tool_calls) {
                if (tc.name === 'view_file' && tc.args.AbsolutePath && tc.args.AbsolutePath.includes('activity_dashboard.xml')) {
                    inViewFile = true;
                    foundFile = false;
                }
            }
        }
        if (inViewFile && obj.type === 'TOOL_CALL_RESPONSE') {
            if (obj.content && obj.content.includes('activity_dashboard.xml')) {
                fs.appendFileSync('original_dashboard_xml.txt', obj.content + '\n==================\n');
                inViewFile = false;
            }
        }
    } catch (e) {}
}
