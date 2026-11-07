/* ============================================================
   MonaHub 前端交互
   - 动效全部可选：GSAP 不存在 / 系统要求减少动态效果时，页面保持纯 CSS 可用
   - 上传：选择/拖入即上传（逐文件 XHR，并发 + 各自进度）
   - 冻结 / 解冻 / 删除 / 重命名共用一个自绘模态
   ============================================================ */
(function () {
    'use strict';

    var reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    var gsap = window.gsap;
    var canAnimate = !reduceMotion && typeof gsap === 'function';

    /* ---------------- 深浅主题 ----------------
       偏好存 localStorage；首屏由 head 内联脚本先落 data-theme 防闪。
       切换优先用 View Transitions 的圆形涟漪（TRead 同款），不支持则直接换。 */
    var THEME_KEY = 'monahub:theme';
    var themeButtons = document.querySelectorAll('[data-theme-toggle]');

    function currentTheme() {
        return document.documentElement.getAttribute('data-theme') === 'light' ? 'light' : 'dark';
    }

    function paintTheme(theme) {
        if (theme === 'light') {
            document.documentElement.setAttribute('data-theme', 'light');
        } else {
            document.documentElement.removeAttribute('data-theme');
        }
        var icon = theme === 'light' ? '☀️' : '🌙';
        var label = theme === 'light' ? '切换到深色主题' : '切换到浅色主题';
        Array.prototype.forEach.call(themeButtons, function (button) {
            button.textContent = icon;
            button.setAttribute('aria-label', label);
            button.setAttribute('title', label);
        });
    }

    function applyTheme(theme) {
        paintTheme(theme);
        try {
            localStorage.setItem(THEME_KEY, theme);
        } catch (ignore) {
            /* 隐私模式下 localStorage 可能抛错，主题仍然生效 */
        }
    }

    function toggleTheme(event) {
        var next = currentTheme() === 'light' ? 'dark' : 'light';
        var run = function () {
            applyTheme(next);
        };
        if (!document.startViewTransition || reduceMotion) {
            run();
            return;
        }
        var x = (event && event.clientX) || window.innerWidth - 40;
        var y = (event && event.clientY) || 40;
        var radius = Math.ceil(Math.hypot(
            Math.max(x, window.innerWidth - x),
            Math.max(y, window.innerHeight - y)));
        var root = document.documentElement;
        root.style.setProperty('--tr-x', x + 'px');
        root.style.setProperty('--tr-y', y + 'px');
        root.style.setProperty('--tr-r', radius + 'px');
        try {
            document.startViewTransition(run);
        } catch (ignore) {
            run();
        }
    }

    paintTheme(currentTheme());
    Array.prototype.forEach.call(themeButtons, function (button) {
        button.addEventListener('click', toggleTheme);
    });

    /* ---------------- 标题逐字入场（split-text） ---------------- */
    Array.prototype.forEach.call(document.querySelectorAll('[data-split]'), function (block) {
        Array.prototype.forEach.call(block.querySelectorAll('.line:not(.grad)'), function (line) {
            var text = line.textContent;
            line.textContent = '';
            Array.prototype.forEach.call(text, function (ch) {
                var span = document.createElement('span');
                span.className = 'ch';
                span.textContent = ch;
                line.appendChild(span);
            });
        });
        if (canAnimate) {
            gsap.from(block.querySelectorAll('.ch'), {
                y: 24,
                opacity: 0,
                duration: 0.6,
                ease: 'power3.out',
                stagger: 0.025,
                delay: 0.08
            });
        }
    });

    /* ---------------- 背景光球视差（floating-orbs） ---------------- */
    var orbs = document.querySelectorAll('.orb');
    if (canAnimate && orbs.length) {
        var movers = [];
        Array.prototype.forEach.call(orbs, function (orb) {
            movers.push({
                x: gsap.quickTo(orb, 'x', { duration: 1.1, ease: 'power3' }),
                y: gsap.quickTo(orb, 'y', { duration: 1.1, ease: 'power3' })
            });
        });
        window.addEventListener('pointermove', function (event) {
            var dx = event.clientX / window.innerWidth - 0.5;
            var dy = event.clientY / window.innerHeight - 0.5;
            movers.forEach(function (mover, index) {
                var amplitude = (index + 1) * 16;
                mover.x(dx * amplitude);
                mover.y(dy * amplitude);
            });
        }, { passive: true });
    }

    /* ---------------- 统计数字递增（number-counter） ---------------- */
    Array.prototype.forEach.call(document.querySelectorAll('[data-count]'), function (el) {
        var target = Number(el.dataset.count || 0);
        if (!canAnimate || !target) {
            el.textContent = String(target);
            return;
        }
        var counter = { value: 0 };
        gsap.to(counter, {
            value: target,
            duration: 1.25,
            ease: 'power2.out',
            onUpdate: function () {
                el.textContent = String(Math.round(counter.value));
            }
        });
    });

    /* ---------------- 列表行入场 ---------------- */
    if (canAnimate) {
        var rows = document.querySelectorAll('.row');
        if (rows.length) {
            gsap.from(rows, {
                y: 14,
                opacity: 0,
                duration: 0.45,
                stagger: 0.035,
                ease: 'power2.out',
                clearProps: 'transform,opacity'
            });
        }
    }

    /* ---------------- 主按钮光斑（liquid-button，委托给 document） ---------------- */
    document.addEventListener('pointermove', function (event) {
        var btn = event.target && event.target.closest ? event.target.closest('.btn-primary') : null;
        if (!btn) {
            return;
        }
        var rect = btn.getBoundingClientRect();
        btn.style.setProperty('--mx', ((event.clientX - rect.left) / rect.width * 100) + '%');
        btn.style.setProperty('--my', ((event.clientY - rect.top) / rect.height * 100) + '%');
    });

    /* ---------------- 登录页小交互 ---------------- */
    var passToggle = document.getElementById('passToggle');
    var passwordInput = document.getElementById('passwordInput');
    if (passToggle && passwordInput) {
        passToggle.addEventListener('click', function () {
            var show = passwordInput.type === 'password';
            passwordInput.type = show ? 'text' : 'password';
            passToggle.textContent = show ? '隐藏' : '显示';
            passToggle.setAttribute('aria-label', show ? '隐藏密码' : '显示密码');
        });
    }

    Array.prototype.forEach.call(document.querySelectorAll('[data-fill-user]'), function (chip) {
        chip.addEventListener('click', function () {
            var userInput = document.querySelector('input[name="username"]');
            var passInput = document.querySelector('input[name="password"]');
            if (userInput) {
                userInput.value = chip.dataset.fillUser;
            }
            if (passInput) {
                passInput.value = chip.dataset.fillPass;
            }
        });
    });

    /* ---------------- flash 提示自动淡出 ---------------- */
    Array.prototype.forEach.call(document.querySelectorAll('[data-toast]'), function (toast) {
        window.setTimeout(function () {
            toast.style.transition = 'opacity .4s ease, transform .4s ease';
            toast.style.opacity = '0';
            toast.style.transform = 'translateY(8px)';
            window.setTimeout(function () {
                if (toast.parentNode) {
                    toast.parentNode.removeChild(toast);
                }
            }, 460);
        }, 3200);
    });

    /* ---------------- 上传区：选择即上传 ---------------- */
    var uploadForm = document.getElementById('uploadForm');
    if (uploadForm) {
        setupUpload(uploadForm);
    }

    function setupUpload(form) {
        var input = document.getElementById('fileInput');
        var chipList = document.getElementById('fileChips');
        var actions = document.getElementById('dzActions');
        var meta = document.getElementById('dzMeta');
        var dropzone = document.getElementById('dropzone');
        var clearBtn = document.getElementById('clearBtn');
        var reloadBtn = document.getElementById('reloadBtn');
        var emptyState = document.getElementById('dzEmpty');

        var selected = [];
        var seq = 0;
        var pending = 0;
        var succeeded = 0;
        var duplicates = 0;
        var finished = false;

        function formatSize(bytes) {
            if (bytes < 1024) {
                return bytes + ' B';
            }
            var kb = bytes / 1024;
            if (kb < 1024) {
                return kb.toFixed(1) + ' KB';
            }
            return (kb / 1024).toFixed(1) + ' MB';
        }

        function createChip(item) {
            var li = document.createElement('li');
            li.className = 'file-chip';
            li.dataset.id = item.id;
            li.innerHTML =
                '<div class="chip-main">' +
                '  <span class="chip-name"></span>' +
                '  <span class="chip-size"></span>' +
                '</div>' +
                '<button type="button" class="chip-remove" aria-label="移除该文件">×</button>' +
                '<div class="chip-bar"><span></span></div>' +
                '<p class="chip-msg"></p>';
            li.querySelector('.chip-name').textContent = item.file.name;
            li.querySelector('.chip-size').textContent = formatSize(item.file.size);
            li.querySelector('.chip-remove').addEventListener('click', function () {
                if (item.state === 'sending') {
                    return;
                }
                selected = selected.filter(function (other) {
                    return other.id !== item.id;
                });
                li.remove();
                sync();
            });
            chipList.appendChild(li);
            return li;
        }

        function updateChip(item) {
            var li = chipList.querySelector('[data-id="' + item.id + '"]');
            if (!li) {
                return;
            }
            if (item.state) {
                li.dataset.state = item.state;
            }
            li.querySelector('.chip-bar span').style.width = (item.progress || 0) + '%';
            li.querySelector('.chip-msg').textContent = item.message || '';
        }

        function sync() {
            var total = selected.reduce(function (sum, item) {
                return sum + item.file.size;
            }, 0);
            actions.hidden = selected.length === 0;
            if (emptyState) {
                emptyState.hidden = selected.length > 0;
            }
            if (selected.length === 0) {
                meta.textContent = '';
                return;
            }
            var parts = [];
            if (pending > 0) {
                parts.push('正在上传…');
            }
            if (succeeded > 0) {
                parts.push(succeeded + ' 成功');
            }
            if (duplicates > 0) {
                parts.push(duplicates + ' 重复已跳过');
            }
            if (finished) {
                var failedCount = selected.length - succeeded - duplicates;
                if (failedCount > 0) {
                    parts.push(failedCount + ' 失败');
                }
            }
            meta.textContent = selected.length + ' 个文件 · 合计 ' + formatSize(total)
                + (parts.length ? ' · ' + parts.join(' / ') : '');
            clearBtn.disabled = pending > 0;
            if (reloadBtn) {
                reloadBtn.hidden = !(finished && succeeded > 0);
            }
        }

        function addFiles(list) {
            var added = [];
            Array.prototype.forEach.call(list, function (file) {
                seq += 1;
                var item = {
                    id: 'f' + seq,
                    file: file,
                    state: '',
                    progress: 0,
                    message: ''
                };
                selected.push(item);
                createChip(item);
                added.push(item);
            });
            if (!added.length) {
                return;
            }
            pending += added.length;
            finished = false;
            sync();
            // 选择即上传：每个文件一个 XHR，天然并发
            added.forEach(uploadOne);
        }

        input.addEventListener('change', function () {
            if (input.files && input.files.length) {
                addFiles(input.files);
                input.value = '';
            }
        });

        ['dragenter', 'dragover'].forEach(function (type) {
            dropzone.addEventListener(type, function (event) {
                event.preventDefault();
                event.stopPropagation();
                dropzone.classList.add('is-dragover');
            });
        });

        ['dragleave', 'drop'].forEach(function (type) {
            dropzone.addEventListener(type, function (event) {
                event.preventDefault();
                event.stopPropagation();
                if (type === 'dragleave' && dropzone.contains(event.relatedTarget)) {
                    return;
                }
                dropzone.classList.remove('is-dragover');
            });
        });

        dropzone.addEventListener('drop', function (event) {
            if (event.dataTransfer && event.dataTransfer.files && event.dataTransfer.files.length) {
                addFiles(event.dataTransfer.files);
            }
        });

        clearBtn.addEventListener('click', function () {
            selected = selected.filter(function (item) {
                return item.state === 'sending';
            });
            Array.prototype.forEach.call(chipList.querySelectorAll('.file-chip'), function (li) {
                var item = selected.find(function (candidate) {
                    return candidate.id === li.dataset.id;
                });
                if (!item) {
                    li.remove();
                }
            });
            succeeded = 0;
            duplicates = 0;
            finished = false;
            sync();
        });

        if (reloadBtn) {
            reloadBtn.addEventListener('click', function () {
                window.location.reload();
            });
        }

        // 无脚本时由浏览器原生提交；有脚本时一切走 addFiles 的自动上传，阻止重复提交
        form.addEventListener('submit', function (event) {
            if (window.XMLHttpRequest) {
                event.preventDefault();
            }
        });

        function uploadOne(item) {
            var xhr = new XMLHttpRequest();
            var data = new FormData();
            data.append('files', item.file, item.file.name);

            item.state = 'sending';
            item.message = '排队上传…';
            updateChip(item);

            xhr.open('POST', form.action, true);
            xhr.setRequestHeader('X-Requested-With', 'XMLHttpRequest');

            xhr.upload.onprogress = function (event) {
                if (event.lengthComputable) {
                    item.progress = Math.round(event.loaded / event.total * 100);
                    item.message = '上传中 ' + item.progress + '%';
                    updateChip(item);
                }
            };

            xhr.onload = function () {
                var body = xhr.responseText || '';
                var result = null;
                if (body.trim().charAt(0) === '{' || body.trim().charAt(0) === '[') {
                    try {
                        var parsed = JSON.parse(body);
                        result = Array.isArray(parsed) ? parsed[0] : parsed;
                    } catch (ignore) {
                        result = null;
                    }
                }

                if (xhr.status === 413) {
                    item.state = 'fail';
                    item.message = '超过大小限制（单个 10MB / 单次 50MB）';
                } else if (xhr.status === 200 && result && result.duplicate) {
                    item.state = 'dup';
                    item.progress = 100;
                    item.message = result.message || '重复文件，已跳过';
                    duplicates += 1;
                } else if (xhr.status === 200 && result && result.ok) {
                    item.state = 'ok';
                    item.progress = 100;
                    item.message = result.message || '上传成功';
                    succeeded += 1;
                } else if (xhr.status === 200 && result) {
                    item.state = 'fail';
                    item.message = result.message || '上传失败';
                } else if (xhr.status === 200 && body.indexOf('<') === 0) {
                    item.state = 'fail';
                    item.message = '登录已过期，请刷新页面重新登录';
                } else {
                    item.state = 'fail';
                    item.message = '上传失败（HTTP ' + xhr.status + '）';
                }
                settle(item);
            };

            xhr.onerror = function () {
                item.state = 'fail';
                item.message = '网络中断，请重试';
                settle(item);
            };

            xhr.send(data);
        }

        function settle(item) {
            updateChip(item);
            pending = Math.max(0, pending - 1);
            if (pending === 0) {
                finished = true;
                if (succeeded > 0 && succeeded + duplicates === selected.length) {
                    meta.textContent = succeeded + ' 个文件上传成功，正在刷新列表…';
                    window.setTimeout(function () {
                        window.location.reload();
                    }, 1200);
                    return;
                }
            }
            sync();
        }
    }

    /* ---------------- 搜索：输入即筛选（250ms 防抖，局部刷新） ---------------- */
    var searchForm = document.querySelector('.search');
    if (searchForm) {
        setupLiveSearch(searchForm);
    }

    function setupLiveSearch(form) {
        var input = form.querySelector('input[name="q"]');
        var list = document.getElementById('fileList');
        if (!input || !list) {
            return;
        }
        var categoryInput = form.querySelector('input[name="category"]');
        var sizeInput = form.querySelector('input[name="size"]');

        var timer = null;
        var controller = null;
        var seq = 0;

        function runSearch() {
            var params = new URLSearchParams();
            params.set('category', categoryInput ? categoryInput.value : 'all');
            params.set('size', sizeInput ? sizeInput.value : '6');
            params.set('page', '1');
            params.set('q', input.value);
            var url = form.action + '?' + params.toString();

            if (controller) {
                controller.abort();
            }
            controller = new AbortController();
            var current = ++seq;
            list.classList.add('is-loading');

            fetch(url, {
                headers: { 'X-Requested-With': 'XMLHttpRequest' },
                credentials: 'same-origin',
                signal: controller.signal
            }).then(function (response) {
                return response.text();
            }).then(function (html) {
                if (current !== seq) {
                    return;
                }
                var doc = new DOMParser().parseFromString(html, 'text/html');
                var fresh = doc.getElementById('fileList');
                if (!fresh) {
                    return;
                }
                list.innerHTML = fresh.innerHTML;
                // 地址栏同步，刷新/收藏后仍是同一个筛选
                window.history.replaceState(null, '', url);
                if (canAnimate) {
                    gsap.from(list.querySelectorAll('.row'), {
                        y: 10,
                        opacity: 0,
                        duration: 0.35,
                        stagger: 0.03,
                        clearProps: 'transform,opacity'
                    });
                }
            }).catch(function (error) {
                if (error.name !== 'AbortError') {
                    console.warn('搜索失败', error);
                }
            }).finally(function () {
                if (current === seq) {
                    list.classList.remove('is-loading');
                }
            });
        }

        input.addEventListener('input', function () {
            window.clearTimeout(timer);
            timer = window.setTimeout(runSearch, 250);
        });

        // 有脚本时回车也走局部刷新；无脚本时表单照常 GET 提交
        form.addEventListener('submit', function (event) {
            event.preventDefault();
            window.clearTimeout(timer);
            runSearch();
        });
    }

    /* ---------------- 点击文件卡片打开文件（本机服务 inline 输出，新标签页） ---------------- */
    document.addEventListener('click', function (event) {
        if (event.button !== 0 || event.defaultPrevented) {
            return;
        }
        var row = event.target.closest ? event.target.closest('.row[data-open-url]') : null;
        if (!row || row.getAttribute('data-frozen') === 'true') {
            return;
        }
        // 不抢链接/按钮/表单的点击；有文本选区时不触发（防止拖选文件名误开）
        if (event.target.closest('a, button, form, input, label, select, textarea')
                || String(window.getSelection() || '').length > 0) {
            return;
        }
        window.open(row.getAttribute('data-open-url'), '_blank');
    });

    /* ---------------- 冻结/重命名/删除是整页刷新：记住并恢复滚动位置 ---------------- */
    try {
        var savedScroll = sessionStorage.getItem('monahub:restoreScroll');
        if (savedScroll !== null) {
            sessionStorage.removeItem('monahub:restoreScroll');
            window.scrollTo(0, parseInt(savedScroll, 10) || 0);
        }
    } catch (ignore) {
    }

    /* ---------------- 冻结 / 解冻 / 删除 / 重命名 共用一个模态 ---------------- */
    var modal = document.getElementById('actionModal');
    if (modal) {
        setupActionModal(modal);
    }

    function setupActionModal(modalEl) {
        var title = document.getElementById('modalTitle');
        var text = document.getElementById('modalText');
        var fileEl = document.getElementById('modalFile');
        var input = document.getElementById('modalInput');
        var hint = document.getElementById('modalHint');
        var okBtn = document.getElementById('modalOk');
        var cancelBtn = document.getElementById('modalCancel');

        var pendingForm = null;
        var mode = '';

        function setConfirmStyle(kind) {
            okBtn.classList.remove('btn-danger', 'btn-primary');
            okBtn.classList.add(kind === 'danger' ? 'btn-danger' : 'btn-primary');
        }

        function openModal() {
            modalEl.hidden = false;
            if (mode === 'rename') {
                input.focus();
                var dot = input.value.lastIndexOf('.');
                input.setSelectionRange(0, dot > 0 ? dot : input.value.length);
            } else {
                cancelBtn.focus();
            }
        }

        function closeModal() {
            modalEl.hidden = true;
            pendingForm = null;
            mode = '';
            input.hidden = true;
            hint.hidden = true;
            fileEl.hidden = false;
        }

        document.addEventListener('submit', function (event) {
            var form = event.target && event.target.closest ? event.target.closest('.js-action-form') : null;
            if (form) {
                event.preventDefault();
                pendingForm = form;
                mode = form.dataset.kind || 'freeze';

                var action = form.dataset.action || '确认';
                title.textContent = action + '该文件？';
                if (mode === 'delete') {
                    text.textContent = '删除后列表与统计中不再显示（逻辑删除，磁盘文件保留）。';
                    setConfirmStyle('danger');
                } else if (action === '解冻') {
                    text.textContent = '解冻后可以继续下载这个文件。';
                    setConfirmStyle('primary');
                } else {
                    text.textContent = '冻结后你自己也无法下载这个文件，可随时解冻。';
                    setConfirmStyle('danger');
                }
                fileEl.textContent = form.dataset.file || '';
                fileEl.hidden = false;
                input.hidden = true;
                hint.hidden = true;
                okBtn.textContent = '确认' + action;
                openModal();
                return;
            }

            var renameForm = event.target && event.target.closest ? event.target.closest('.js-rename-form') : null;
            if (renameForm) {
                event.preventDefault();
                pendingForm = renameForm;
                mode = 'rename';

                title.textContent = '重命名文件';
                text.textContent = '只改显示名，磁盘文件名不动；扩展名不能修改。';
                fileEl.textContent = '';
                fileEl.hidden = true;
                input.hidden = false;
                input.value = renameForm.dataset.file || '';
                hint.hidden = false;
                hint.textContent = '扩展名保持 .' + (renameForm.dataset.ext || '');
                okBtn.textContent = '保存';
                setConfirmStyle('primary');
                openModal();
            }
        });

        cancelBtn.addEventListener('click', closeModal);
        modalEl.addEventListener('click', function (event) {
            if (event.target === modalEl) {
                closeModal();
            }
        });
        document.addEventListener('keydown', function (event) {
            if (event.key === 'Escape' && !modalEl.hidden) {
                closeModal();
            }
        });

        okBtn.addEventListener('click', function () {
            if (!pendingForm) {
                return;
            }
            if (mode === 'rename') {
                var name = (input.value || '').trim();
                if (!name) {
                    hint.hidden = false;
                    hint.textContent = '文件名不能为空';
                    input.focus();
                    return;
                }
                var hidden = pendingForm.querySelector('input[name="newName"]');
                if (hidden) {
                    hidden.value = name;
                }
            }
            var form = pendingForm;
            closeModal();
            try {
                sessionStorage.setItem('monahub:restoreScroll', String(window.scrollY || 0));
            } catch (ignore) {
            }
            form.submit();
        });
    }
})();
