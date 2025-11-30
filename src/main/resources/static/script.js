document.addEventListener('DOMContentLoaded', () => {
    const tabButtons = document.querySelectorAll('.tab-button');
    const tabContents = document.querySelectorAll('.tab-content');
    const fileForm = document.getElementById('file-form');
    const pasteForm = document.getElementById('paste-form');
    const resultsContainer = document.getElementById('results-container');
    const reportDiv = document.getElementById('report');

    tabButtons.forEach(button => {
        button.addEventListener('click', () => {
            tabButtons.forEach(btn => btn.classList.remove('active'));
            button.classList.add('active');
            tabContents.forEach(content => content.classList.remove('active'));
            const tabId = button.getAttribute('data-tab');
            document.getElementById(tabId + '-form').classList.add('active');
        });
    });

    fileForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const file = document.getElementById('email-file-input').files[0];
        if (!file) return alert('Please select a file.');
        const formData = new FormData();
        formData.append('emailFile', file);
        await performAnalysis('/api/v1/analyze/email', { method: 'POST', body: formData });
    });

    pasteForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const content = document.getElementById('email-content-input').value;
        if (!content.trim()) return alert('Please paste content.');
        await performAnalysis('/api/v1/analyze/email-content', {
            method: 'POST',
            headers: { 'Content-Type': 'text/plain' },
            body: content
        });
    });

    async function performAnalysis(endpoint, options) {
        resultsContainer.classList.remove('hidden');
        reportDiv.innerHTML = '<p style="text-align:center; color:#888;">🦅 Phish-Hawk AI is scanning...</p>';
        try {
            const response = await fetch(endpoint, options);
            if (!response.ok) throw new Error(`Server Error: ${response.status}`);
            const result = await response.json();
            renderDashboard(result);
        } catch (error) {
            reportDiv.innerHTML = `<div class="ai-card spam"><h3>Error</h3><p>${error.message}</p></div>`;
        }
    }

    function renderDashboard(data) {
        let html = '';
        if (data.aiEnabled) {
            const isSpam = data.aiSuspicious;
            const theme = isSpam ? 'spam' : 'safe';
            const icon = isSpam ? '🚨' : '✅';
            let confVal = parseFloat(data.aiConfidence.replace('%',''));
            if(isNaN(confVal)) confVal = 0;
            html += `
            <div class="ai-card ${theme}">
                <div class="verdict-row">
                    <span class="verdict-label">AI Intelligence Verdict</span>
                    <span class="verdict-badge ${theme}">${icon} ${data.aiVerdict}</span>
                </div>
                <div class="confidence-container">
                    <div class="progress-bg">
                        <div class="progress-fill ${theme}" style="width: ${confVal}%"></div>
                    </div>
                    <span class="confidence-text">AI Confidence: ${data.aiConfidence}</span>
                </div>
            </div>`;
        } else {
            html += `<div class="ai-card"><p>⚠️ AI Service Unavailable (Running in Offline Mode)</p></div>`;
        }
        html += `<div class="tech-details">`;
        html += `<div class="detail-row"><span class="detail-label">Analysis Summary</span><div class="detail-value">${data.analysisSummary}</div></div>`;
        html += `<div class="detail-row"><span class="detail-label">Sender Info</span><div class="detail-value">From: ${data.fromHeader || 'N/A'}</div><div class="detail-value" style="margin-top:5px; font-size:0.85rem; color:#888;">Return-Path: ${data.returnPathHeader || 'N/A'}</div></div>`;
        const links = data.foundUrls || [];
        html += `<div class="detail-row"><span class="detail-label">Suspicious Links Found (${links.length})</span>`;
        if (links.length > 0) {
            html += `<ul>${links.map(l => `<li>${l}</li>`).join('')}</ul>`;
        } else {
            html += `<div class="detail-value">None found.</div>`;
        }
        html += `</div></div>`;
        reportDiv.innerHTML = html;
    }
});