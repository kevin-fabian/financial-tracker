---
name: thymeleaf-ui-pattern-form-ux
description: A guide for consistent Thymeleaf template structure, Tailwind styling, form binding, validation patterns, and user-friendly content in `app/src/main/resources/templates`.
---

Use this guide for all HTML templates in `app/src/main/resources/templates`.

### Theme & Color Palette

**Primary Color:** Amber (Yellow)

| Purpose              | Tailwind Class          |
|----------------------|-------------------------|
| Primary              | `amber-400`             |
| Primary Hover        | `amber-500`             |
| Primary Focus/Ring   | `amber-600`             |
| Primary Light        | `amber-50` / `amber-100`|
| Neutral Background   | `slate-50`              |
| Text Dark            | `slate-900` / `slate-800` | 
| Text Muted           | `slate-600` / `slate-500` |

**Usage Rules:**
- Use `amber-400` as the main brand color for buttons, links, and focus states.
- For light accents: `amber-50`, `amber-100`, `border-amber-100`, `ring-amber-50`.
- Update the primary color in this guide + `fragments/ui.html` when changing the theme in the future.

### Default Page Skeleton (Recommended)

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org" 
      lang="en"
      th:replace="~{layouts/base :: layout(~{::content}, ~{::scripts})}">

<div th:fragment="content">
    <div class="min-h-screen bg-slate-50 flex items-center justify-center py-8 px-4">
        <div class="w-full max-w-md bg-white shadow-xl rounded-2xl p-8 sm:p-10">
            
            <!-- Page Header -->
            <div class="text-center mb-8">
                <h1 class="text-3xl font-semibold tracking-tight text-slate-900">Page Title</h1>
                <p class="mt-3 text-slate-600 text-sm">Supportive description text here.</p>
            </div>

            <!-- Form / Content goes here -->

        </div>
    </div>
</div>

<div th:fragment="scripts">
    <!-- Page-specific JS only when needed -->
    <script th:src="@{/js/auth.js}"></script>
</div>
</html>
```

### Tailwind Design System & Practical Classes

#### 1. Layout & Containers
- **Main centered card**: `w-full max-w-md bg-white shadow-xl rounded-2xl p-8 sm:p-10`
- **Page wrapper**: `min-h-screen bg-slate-50 flex items-center justify-center py-8 px-4`
- **Form spacing**: `space-y-6`

#### 2. Typography
- **Main title**: `text-3xl font-semibold tracking-tight text-slate-900`
- **Subtitle**: `text-xl font-medium text-slate-800`
- **Support text**: `text-sm text-slate-600`
- **Helper / caption**: `text-sm text-slate-500`
- **Field label**: `block text-sm font-medium text-slate-700 mb-2`

#### 3. Form Fields

```html
<label for="email" class="block text-sm font-medium text-slate-700 mb-2">
    Email address
</label>
<input 
    id="email"
    th:field="*{email}"
    type="email"
    class="w-full px-4 py-3 bg-white border border-slate-300 rounded-xl 
           focus:outline-none focus:border-amber-600 focus:ring-1 focus:ring-amber-600 
           placeholder:text-slate-400 transition-all duration-200
           disabled:bg-slate-100 disabled:text-slate-400"
    placeholder="you@example.com"
    autocomplete="email"
    required>
```

#### 4. Buttons

**Primary Button (Amber):**
```html
<button type="submit"
        class="w-full py-4 px-6 bg-amber-400 hover:bg-amber-500 active:bg-amber-800 
               text-white font-medium rounded-2xl transition-all duration-200 
               focus:outline-none focus:ring-2 focus:ring-amber-600 focus:ring-offset-2
               flex items-center justify-center gap-2">
    Continue
</button>
```

**Secondary Button:** `text-slate-700 hover:bg-slate-100 font-medium px-5 py-3 rounded-2xl transition-colors`

**Link style:** `text-amber-400 hover:text-amber-500 font-medium hover:underline`

#### 5. Alerts & Messages

- Success: `bg-emerald-50 border border-emerald-100 text-emerald-800 rounded-2xl p-4 text-sm`
- Error: `bg-red-50 border border-red-100 text-red-700 rounded-2xl p-4 text-sm`
- Info / Neutral: `bg-amber-50 border border-amber-100 text-amber-800 rounded-2xl p-4 text-sm`

### Recommended Reusable UI Fragments

Create `fragments/ui.html` for consistency:

```html
<!-- fragments/ui.html -->
<div th:fragment="primaryButton(label, type='button')">
    <button th:type="${type}" class="w-full py-4 px-6 bg-amber-400 hover:bg-amber-500 ...">
        <span th:text="${label}"></span>
    </button>
</div>

<div th:fragment="primaryLink(href, label)">
    <a th:href="${href}" 
       class="block w-full text-center py-4 px-6 bg-amber-400 hover:bg-amber-500 text-white font-medium rounded-2xl">
        <span th:text="${label}"></span>
    </a>
</div>

<div th:fragment="statusHeader(title, subtitle, tone='success')">
    <!-- Implement different tones: success, error, neutral using amber/emerald/red -->
</div>
```

Example usage (as per your provided template):
```html
<div th:replace="~{fragments/ui :: statusHeader(title='Registration successful', subtitle='Your account details are ready.', tone='success')}"></div>
```

### Form Binding & Validation

```html
<form th:action="@{/auth/login}" 
      th:object="${loginForm}" 
      method="post" 
      class="space-y-6"
      novalidate>
    <!-- fields + errors using fragments/alerts -->
</form>
```

### Accessibility & UX Checklist

- Every input has a proper `<label>`
- Strong focus indicators using `amber-600`
- Clear action-oriented button text
- Logical tab order and keyboard navigation
- Helpful placeholders

### JavaScript & Email Templates

- JS: Minimal, included via `scripts` fragment.
- Email templates: Use inline CSS + tables (no Tailwind).

### New Template Workflow

1. Start with the default skeleton.
2. Use amber-based primary styles from this guide.
3. Leverage fragments from `fragments/ui.html` and `fragments/alerts.html`.
4. Test responsiveness and accessibility.

### PR Checklist

- [ ] Uses recommended page skeleton and amber theme colors
- [ ] Primary actions use `amber-400` / `amber-500`
- [ ] Follows documented Tailwind patterns
- [ ] Uses `th:object` + `th:field`
- [ ] Errors rendered via fragments
- [ ] Accessibility requirements met
- [ ] Clean, friendly, security-conscious copy
