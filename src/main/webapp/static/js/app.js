/* ============================================================
   MonaHub 前端交互
   - 动效全部可选：GSAP 不存在 / 系统要求减少动态效果时，页面保持纯 CSS 可用
   - 上传走逐文件 XHR（并发），同时保留无脚本时的原生表单提交
   ============================================================ */
(function () {
    'use strict';

    var body = document.body;
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

    /* ---------------- 主按钮光斑（liquid-button） ---------------- */
    Array.prototype.forEach.call(document.querySelectorAll('.btn-primary'), function (btn) {
        btn.addEventListener('pointermove', function (event) {
            var rect = btn.getBoundingClientRect();
            btn.style.setProperty('--mx', ((event.clientX - rect.left) / rect.width * 100) + '%');
            btn.style.setProperty('--my', ((event.clientY - rect.top) / rect.height * 100) + '%');
        });
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

    /* ---------------- 上传区 ---------------- */
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
        var uploadBtn = document.getElementById('uploadBtn');
        var emptyState = document.getElementById('dzEmpty');
        var selected = [];
        var seq = 0;

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
            meta.textContent = selected.length
                ? selected.length + ' 个文件 · 合计 ' + formatSize(total)
                : '';
        }

        function addFiles(list) {
            Array.prototype.forEach.call(list, function (file) {
                seq += 1;
                selected.push({
                    id: 'f' + seq,
                    file: file,
                    state: '',
                    progress: 0,
                    message: ''
                });
                createChip(selected[selected.length - 1]);
            });
            sync();
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
            sync();
        });

        form.addEventListener('submit', function (event) {
            if (!selected.length) {
                event.preventDefault();
                return;
            }
            if (!window.XMLHttpRequest) {
                return; // 老浏览器：交给原生表单提交
            }
            event.preventDefault();
            uploadAll();
        });

        function uploadAll() {
            uploadBtn.disabled = true;
            uploadBtn.classList.add('is-disabled');
            clearBtn.disabled = true;
            meta.textContent = '正在上传 ' + selected.length + ' 个文件…';

            var tasks = selected.map(uploadOne);
            Promise.all(tasks).then(function (results) {
                var ok = results.filter(Boolean).length;
                if (ok > 0) {
                    meta.textContent = ok + ' 个文件上传成功，正在刷新列表…';
                    window.setTimeout(function () {
                        window.location.reload();
                    }, 1300);
                } else {
                    meta.textContent = '没有文件上传成功，请查看每个文件的原因';
                    uploadBtn.disabled = false;
                    uploadBtn.classList.remove('is-disabled');
                    clearBtn.disabled = false;
                }
            });
        }

        function uploadOne(item) {
            return new Promise(function (resolve) {
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
                    } else if (xhr.status === 200 && result && result.ok) {
                        item.state = 'ok';
                        item.progress = 100;
                        item.message = result.message || '上传成功';
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
                    updateChip(item);
                    resolve(item.state === 'ok');
                };

                xhr.onerror = function () {
                    item.state = 'fail';
                    item.message = '网络中断，请重试';
                    updateChip(item);
                    resolve(false);
                };

                xhr.send(data);
            });
        }
    }

    /* ---------------- 冻结/解冻确认模态 ---------------- */
    var modal = document.getElementById('freezeModal');
    if (modal) {
        setupFreezeModal(modal);
    }

    function setupFreezeModal(modalEl) {
        var title = document.getElementById('modalTitle');
        var text = document.getElementById('modalText');
        var fileEl = document.getElementById('modalFile');
        var okBtn = document.getElementById('modalOk');
        var cancelBtn = document.getElementById('modalCancel');
        var pendingForm = null;

        Array.prototype.forEach.call(document.querySelectorAll('.freeze-form'), function (form) {
            form.addEventListener('submit', function (event) {
                event.preventDefault();
                pendingForm = form;
                var action = form.dataset.action || '冻结';
                title.textContent = action + '该文件？';
                text.textContent = action === '冻结'
                    ? '冻结后你自己也无法下载这个文件，可随时解冻。'
                    : '解冻后可以继续下载这个文件。';
                fileEl.textContent = form.dataset.file || '';
                okBtn.textContent = '确认' + action;
                okBtn.classList.toggle('btn-danger', action === '冻结');
                modalEl.hidden = false;
                cancelBtn.focus();
            });
        });

        function close() {
            modalEl.hidden = true;
            pendingForm = null;
        }

        cancelBtn.addEventListener('click', close);
        modalEl.addEventListener('click', function (event) {
            if (event.target === modalEl) {
                close();
            }
        });
        document.addEventListener('keydown', function (event) {
            if (event.key === 'Escape' && !modalEl.hidden) {
                close();
            }
        });
        okBtn.addEventListener('click', function () {
            if (!pendingForm) {
                return;
            }
            var form = pendingForm;
            close();
            form.submit();
        });
    }
})();
