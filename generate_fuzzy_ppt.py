import sys
from pptx import Presentation
from pptx.util import Inches, Pt
from pptx.enum.text import PP_ALIGN, MSO_ANCHOR
from pptx.dml.color import RGBColor
from pptx.enum.shapes import MSO_SHAPE

def create_presentation(filename="Fuzzy_Logic_and_Syadvada_Project.pptx"):
    prs = Presentation()
    # Set 16:9 widescreen dimensions
    prs.slide_width = Inches(13.333)
    prs.slide_height = Inches(7.5)
    blank_layout = prs.slide_layouts[6] # blank slide layout

    # Color Palette
    COLOR_BG_DARK = RGBColor(15, 23, 42)      # Deep Navy #0F172A
    COLOR_CARD_DARK = RGBColor(30, 41, 59)    # Slate #1E293B
    COLOR_BG_LIGHT = RGBColor(248, 250, 252)  # Soft Light #F8FAFC
    COLOR_CARD_LIGHT = RGBColor(255, 255, 255)# White
    COLOR_BORDER_LIGHT = RGBColor(226, 232, 240) # Border #E2E8F0
    
    COLOR_PRIMARY = RGBColor(79, 70, 229)     # Indigo #4F46E5
    COLOR_PRIMARY_LIGHT = RGBColor(238, 242, 255) # Light Indigo
    COLOR_ACCENT_ROSE = RGBColor(225, 29, 72) # Rose #E11D48
    COLOR_ACCENT_TEAL = RGBColor(13, 148, 136)# Teal #0D9488
    COLOR_ACCENT_AMBER = RGBColor(217, 119, 6)# Amber #D97706
    COLOR_TEXT_MAIN = RGBColor(15, 23, 42)    # Dark Slate
    COLOR_TEXT_MUTED = RGBColor(100, 116, 139)# Muted Gray #64748B
    COLOR_TEXT_LIGHT = RGBColor(248, 250, 252)# Off-white
    COLOR_TEXT_SUBTLE = RGBColor(148, 163, 184) # Light Slate

    def add_header(slide, title_text, category_text, dark=False):
        # Category Tag / Breadcrumb
        tag_box = slide.shapes.add_textbox(Inches(0.8), Inches(0.4), Inches(11.7), Inches(0.35))
        tf_tag = tag_box.text_frame
        tf_tag.word_wrap = True
        tf_tag.margin_left = tf_tag.margin_top = tf_tag.margin_right = tf_tag.margin_bottom = 0
        p_tag = tf_tag.paragraphs[0]
        p_tag.text = category_text.upper()
        p_tag.font.size = Pt(11)
        p_tag.font.bold = True
        p_tag.font.color.rgb = COLOR_PRIMARY if not dark else RGBColor(165, 180, 252)

        # Main Title
        title_box = slide.shapes.add_textbox(Inches(0.8), Inches(0.75), Inches(11.7), Inches(0.7))
        tf_title = title_box.text_frame
        tf_title.word_wrap = True
        tf_title.margin_left = tf_title.margin_top = tf_title.margin_right = tf_title.margin_bottom = 0
        p_title = tf_title.paragraphs[0]
        p_title.text = title_text
        p_title.font.size = Pt(24)
        p_title.font.bold = True
        p_title.font.color.rgb = COLOR_TEXT_MAIN if not dark else COLOR_TEXT_LIGHT

    def create_card(slide, left, top, width, height, bg_color=COLOR_CARD_LIGHT, border_color=COLOR_BORDER_LIGHT):
        shape = slide.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, left, top, width, height)
        shape.fill.solid()
        shape.fill.fore_color.rgb = bg_color
        if border_color:
            shape.line.color.rgb = border_color
            shape.line.width = Pt(1.5)
        else:
            shape.line.fill.background()
        return shape

    # ==========================================
    # SLIDE 1: TITLE SLIDE (Dark Premium Theme)
    # ==========================================
    s1 = prs.slides.add_slide(blank_layout)
    bg1 = s1.shapes.add_shape(MSO_SHAPE.RECTANGLE, 0, 0, prs.slide_width, prs.slide_height)
    bg1.fill.solid()
    bg1.fill.fore_color.rgb = COLOR_BG_DARK
    bg1.line.fill.background()

    # Pill badge
    badge = s1.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(1.2), Inches(1.1), Inches(4.5), Inches(0.45))
    badge.fill.solid()
    badge.fill.fore_color.rgb = COLOR_CARD_DARK
    badge.line.color.rgb = COLOR_PRIMARY
    badge.line.width = Pt(1)
    tf_b = badge.text_frame
    p_b = tf_b.paragraphs[0]
    p_b.text = "INTERDISCIPLINARY EXHIBITION & WORKING MODEL"
    p_b.font.size = Pt(11)
    p_b.font.bold = True
    p_b.font.color.rgb = RGBColor(165, 180, 252)
    p_b.alignment = PP_ALIGN.CENTER

    # Title box
    tbox = s1.shapes.add_textbox(Inches(1.2), Inches(1.8), Inches(11.0), Inches(2.3))
    tf = tbox.text_frame
    tf.word_wrap = True
    p1 = tf.paragraphs[0]
    p1.text = "Beyond Black & White:\nFuzzy Logic & Syādvāda"
    p1.font.size = Pt(44)
    p1.font.bold = True
    p1.font.color.rgb = COLOR_TEXT_LIGHT

    # Subtitle box
    sbox = s1.shapes.add_textbox(Inches(1.2), Inches(4.3), Inches(10.5), Inches(1.2))
    stf = sbox.text_frame
    stf.word_wrap = True
    sp1 = stf.paragraphs[0]
    sp1.text = "Bridging Modern Soft Computing with Ancient Indian Epistemology (Anekāntavāda)"
    sp1.font.size = Pt(20)
    sp1.font.color.rgb = RGBColor(203, 213, 225)
    sp2 = stf.add_paragraph()
    sp2.text = "An interactive, visual demonstration project exploring multi-valued truth, human perception, and smart machines."
    sp2.font.size = Pt(15)
    sp2.font.color.rgb = COLOR_TEXT_SUBTLE

    # Bottom 3 Highlight Cards
    highlights = [
        ("POSTER 1: LOGIC", "Classical vs. Fuzzy: 'Is this tall?' 'Is it hot?'"),
        ("POSTER 2: PHILOSOPHY", "Syādvāda & The Sevenfold Predication (Saptabhaṅgī)"),
        ("INTERACTIVE DEMO", "Movable pointers, Visitor slider & Fuzzy Washing Machine")
    ]
    card_w = Inches(3.45)
    card_gap = Inches(0.3)
    start_x = Inches(1.2)
    for i, (head, desc) in enumerate(highlights):
        c = create_card(s1, start_x + i * (card_w + card_gap), Inches(5.7), card_w, Inches(1.2), bg_color=COLOR_CARD_DARK, border_color=COLOR_PRIMARY)
        ctf = c.text_frame
        ctf.word_wrap = True
        ctf.margin_top = Inches(0.18)
        cp1 = ctf.paragraphs[0]
        cp1.text = head
        cp1.font.size = Pt(11)
        cp1.font.bold = True
        cp1.font.color.rgb = RGBColor(165, 180, 252)
        cp2 = ctf.add_paragraph()
        cp2.text = desc
        cp2.font.size = Pt(12)
        cp2.font.color.rgb = COLOR_TEXT_LIGHT

    s1.notes_slide.notes_text_frame.text = (
        "Welcome judges and visitors! This project demonstrates how everyday human reality is rarely binary (true or false). "
        "We unite Lotfi Zadeh's 1965 Fuzzy Logic with the ancient 6th-century BCE Jain philosophy of Syādvāda (conditional predication) "
        "through interactive posters, working physical/digital models, and audience experiments."
    )

    # ==========================================
    # SLIDE 2: PROJECT BLUEPRINT & EXHIBITION ROADMAP
    # ==========================================
    s2 = prs.slides.add_slide(blank_layout)
    add_header(s2, "Project Blueprint: Interactive Exhibition Architecture", "Exhibition Overview")

    steps = [
        ("Poster 1: Foundations", "Classical vs. Fuzzy Logic", [
            "Aristotle's Law of Excluded Middle vs. Degrees of Truth [0, 1].",
            "Everyday quandaries: 'Is 5'10\" tall?' 'Is 26°C hot?'",
            "Sharp cliff threshold vs. smooth membership curves."
        ], COLOR_ACCENT_ROSE),
        ("Poster 2: The Philosophical Link", "Syādvāda & Anekāntavāda", [
            "Anekāntavāda: Non-absolutism & multi-faceted reality.",
            "The concept of 'Syāt' (in some respects / from a standpoint).",
            "Saptabhaṅgī: The 7-fold predication translated to modern logic."
        ], COLOR_ACCENT_AMBER),
        ("Live Models & Demos", "Hands-on Engineering", [
            "Chart paper board with movable pointers & dual-membership.",
            "Smart Fuzzy Washing Machine & AC Temperature controller.",
            "Real-life applications matrix across consumer & transport tech."
        ], COLOR_ACCENT_TEAL),
        ("Audience Engagement", "The Human Fuzzy Set Experiment", [
            "Visitors rate 'Tall', 'Sweet', 'Intelligent' on 0.0 to 1.0 scales.",
            "Instant curve plotting reveals personal fuzzy thresholds.",
            "Takeaway: Humans naturally compute using fuzzy sets!"
        ], COLOR_PRIMARY)
    ]

    col_w = Inches(2.75)
    gap = Inches(0.25)
    for i, (badge_txt, title, bullets, acc_col) in enumerate(steps):
        x = Inches(0.8) + i * (col_w + gap)
        c = create_card(s2, x, Inches(1.6), col_w, Inches(5.3), bg_color=COLOR_CARD_LIGHT, border_color=COLOR_BORDER_LIGHT)
        
        # Color bar top
        bar = s2.shapes.add_shape(MSO_SHAPE.RECTANGLE, x, Inches(1.6), col_w, Inches(0.12))
        bar.fill.solid()
        bar.fill.fore_color.rgb = acc_col
        bar.line.fill.background()

        ctf = c.text_frame
        ctf.word_wrap = True
        ctf.margin_top = Inches(0.25)
        ctf.margin_left = ctf.margin_right = Inches(0.2)
        
        p_badge = ctf.paragraphs[0]
        p_badge.text = badge_txt.upper()
        p_badge.font.size = Pt(10)
        p_badge.font.bold = True
        p_badge.font.color.rgb = acc_col

        p_t = ctf.add_paragraph()
        p_t.text = title
        p_t.font.size = Pt(15)
        p_t.font.bold = True
        p_t.font.color.rgb = COLOR_TEXT_MAIN

        for b in bullets:
            pb = ctf.add_paragraph()
            pb.text = "• " + b
            pb.font.size = Pt(11)
            pb.font.color.rgb = COLOR_TEXT_MUTED

    s2.notes_slide.notes_text_frame.text = (
        "Explain to the judges how this booth is arranged: visitors start with intuitive everyday questions (Poster 1), "
        "connect it with profound Indian epistemological roots (Poster 2), test physical sliders and digital simulators, "
        "and finally record their own perception on the interactive rating board."
    )

    # ==========================================
    # SLIDE 3: POSTER 1 — CLASSICAL VS FUZZY LOGIC
    # ==========================================
    s3 = prs.slides.add_slide(blank_layout)
    add_header(s3, "Poster 1: Classical (Binary) vs. Fuzzy Logic", "Poster 1 Breakdown • The Core Duality")

    # Column Left: Classical Logic
    c_left = create_card(s3, Inches(0.8), Inches(1.6), Inches(5.6), Inches(5.3), bg_color=RGBColor(254, 242, 242), border_color=COLOR_ACCENT_ROSE)
    ctf_l = c_left.text_frame
    ctf_l.word_wrap = True
    ctf_l.margin_left = ctf_l.margin_right = ctf_l.margin_top = Inches(0.3)

    p_lh = ctf_l.paragraphs[0]
    p_lh.text = "CLASSICAL (CRISP / BINARY) LOGIC"
    p_lh.font.size = Pt(13)
    p_lh.font.bold = True
    p_lh.font.color.rgb = COLOR_ACCENT_ROSE

    p_lt = ctf_l.add_paragraph()
    p_lt.text = "Aristotelian Truth: Strictly True (1) or False (0)"
    p_lt.font.size = Pt(17)
    p_lt.font.bold = True
    p_lt.font.color.rgb = COLOR_TEXT_MAIN

    l_items = [
        ("The 'Excluded Middle' Axiom", "A proposition is either completely True or completely False. No middle ground permitted."),
        ("The 'Cliff' Problem", "Sharp, artificial cutoffs. If 'Tall' is defined as >= 6'0\":\n- 6'0\" is 100% Tall (1)\n- 5'11.9\" is 0% Tall (0) — absurd in reality!"),
        ("Weather Dilemma", "If 'Hot' >= 30°C: 29.9°C is Cold, 30.0°C is Hot. Digital switches force unnatural rigidity."),
        ("Limitation", "Computers think in crisp bits (0/1), but the physical world and human reasoning operate in nuances.")
    ]
    for h, desc in l_items:
        ph = ctf_l.add_paragraph()
        ph.text = "✖ " + h
        ph.font.size = Pt(12)
        ph.font.bold = True
        ph.font.color.rgb = COLOR_ACCENT_ROSE
        pd = ctf_l.add_paragraph()
        pd.text = desc
        pd.font.size = Pt(11)
        pd.font.color.rgb = COLOR_TEXT_MUTED

    # Column Right: Fuzzy Logic
    c_right = create_card(s3, Inches(6.8), Inches(1.6), Inches(5.6), Inches(5.3), bg_color=RGBColor(240, 253, 250), border_color=COLOR_ACCENT_TEAL)
    ctf_r = c_right.text_frame
    ctf_r.word_wrap = True
    ctf_r.margin_left = ctf_r.margin_right = ctf_r.margin_top = Inches(0.3)

    p_rh = ctf_r.paragraphs[0]
    p_rh.text = "FUZZY LOGIC (LOTFI ZADEH, 1965)"
    p_rh.font.size = Pt(13)
    p_rh.font.bold = True
    p_rh.font.color.rgb = COLOR_ACCENT_TEAL

    p_rt = ctf_r.add_paragraph()
    p_rt.text = "Graded Truth: Degrees of Membership [0.0 to 1.0]"
    p_rt.font.size = Pt(17)
    p_rt.font.bold = True
    p_rt.font.color.rgb = COLOR_TEXT_MAIN

    r_items = [
        ("Continuous Truth Spectrum", "Truth is not black/white; membership function μA(x) assigns a degree of truth from 0.0 to 1.0."),
        ("Natural Transitions ('Tall')", "Height has overlapping fuzzy boundaries:\n- 5'6\" = 0.2 Tall / 0.8 Average\n- 5'10\" = 0.65 Tall / 0.35 Average\n- 6'2\" = 1.0 Tall"),
        ("Natural Weather Assessment", "28°C is simultaneously: 0.70 'Warm' AND 0.30 'Hot'. Smooth, human-like reasoning."),
        ("Engineering Power", "Enables microcontrollers to make smooth, optimal decisions without jerky threshold oscillations.")
    ]
    for h, desc in r_items:
        ph = ctf_r.add_paragraph()
        ph.text = "✔ " + h
        ph.font.size = Pt(12)
        ph.font.bold = True
        ph.font.color.rgb = COLOR_ACCENT_TEAL
        pd = ctf_r.add_paragraph()
        pd.text = desc
        pd.font.size = Pt(11)
        pd.font.color.rgb = COLOR_TEXT_MUTED

    s3.notes_slide.notes_text_frame.text = (
        "Point to Poster 1 on your exhibition wall. Ask the visitor: 'If a man is 5 feet 11.5 inches, is he tall?' "
        "Under classical logic, you are forced to say YES or NO. If your cutoff is 6 feet, he is 0% tall. "
        "Fuzzy logic solves this by saying he belongs to the set 'Tall' to degree 0.85."
    )

    # ==========================================
    # SLIDE 4: POSTER 1 — VISUALIZING FUZZY CURVES
    # ==========================================
    s4 = prs.slides.add_slide(blank_layout)
    add_header(s4, "Poster 1: Membership Functions & Real-Life Scales", "Visualizing Graded Truth")

    # Left visual card: The Mathematics of Nuance
    c_math = create_card(s4, Inches(0.8), Inches(1.6), Inches(4.5), Inches(5.3), bg_color=COLOR_CARD_LIGHT)
    ctf_m = c_math.text_frame
    ctf_m.word_wrap = True
    ctf_m.margin_left = ctf_m.margin_right = ctf_m.margin_top = Inches(0.3)
    
    p = ctf_m.paragraphs[0]
    p.text = "THE MEMBERSHIP FUNCTION"
    p.font.size = Pt(12)
    p.font.bold = True
    p.font.color.rgb = COLOR_PRIMARY

    p = ctf_m.add_paragraph()
    p.text = "μ(x) : X → [0, 1]"
    p.font.size = Pt(20)
    p.font.bold = True
    p.font.color.rgb = COLOR_TEXT_MAIN

    math_points = [
        ("Triangular & Trapezoidal Sets", "The most common shapes used in engineering because they are computationally fast and intuitive."),
        ("Not Probability, But Vagueness", "Probability = likelihood of an event occurring (will it rain tomorrow?).\nFuzziness = degree of truth of a concept (is 22°C 'warm'?)."),
        ("Linguistic Variables", "Fuzzy logic allows computers to process words like 'slightly slow', 'moderately hot', and 'very fast' directly.")
    ]
    for h, d in math_points:
        ph = ctf_m.add_paragraph()
        ph.text = "• " + h
        ph.font.size = Pt(13)
        ph.font.bold = True
        ph.font.color.rgb = COLOR_PRIMARY
        pd = ctf_m.add_paragraph()
        pd.text = d
        pd.font.size = Pt(11)
        pd.font.color.rgb = COLOR_TEXT_MUTED

    # Right 2 Cards: Everyday Examples (Tall & Hot)
    c_ex1 = create_card(s4, Inches(5.6), Inches(1.6), Inches(6.9), Inches(2.5), bg_color=COLOR_PRIMARY_LIGHT, border_color=COLOR_PRIMARY)
    ctf_e1 = c_ex1.text_frame
    ctf_e1.word_wrap = True
    ctf_e1.margin_left = ctf_e1.margin_right = ctf_e1.margin_top = Inches(0.25)
    
    p = ctf_e1.paragraphs[0]
    p.text = "CASE STUDY 1: 'IS THIS PERSON TALL?'"
    p.font.size = Pt(12)
    p.font.bold = True
    p.font.color.rgb = COLOR_PRIMARY

    p = ctf_e1.add_paragraph()
    p.text = "Height = 5'10\" (178 cm)"
    p.font.size = Pt(16)
    p.font.bold = True
    p.font.color.rgb = COLOR_TEXT_MAIN

    p = ctf_e1.add_paragraph()
    p.text = "• Classical Logic Cutoff (6'0\"): False [0] — treated identically to someone 4'10\"!\n• Fuzzy Logic Membership:\n  - μ_Short(5'10\") = 0.00\n  - μ_Average(5'10\") = 0.40\n  - μ_Tall(5'10\") = 0.60\nConclusion: Smoothly belongs to both 'Average' and 'Tall' with differing weights."
    p.font.size = Pt(11)
    p.font.color.rgb = COLOR_TEXT_MAIN

    c_ex2 = create_card(s4, Inches(5.6), Inches(4.3), Inches(6.9), Inches(2.6), bg_color=RGBColor(254, 243, 199), border_color=COLOR_ACCENT_AMBER)
    ctf_e2 = c_ex2.text_frame
    ctf_e2.word_wrap = True
    ctf_e2.margin_left = ctf_e2.margin_right = ctf_e2.margin_top = Inches(0.25)

    p = ctf_e2.paragraphs[0]
    p.text = "CASE STUDY 2: 'IS THE WEATHER HOT?'"
    p.font.size = Pt(12)
    p.font.bold = True
    p.font.color.rgb = COLOR_ACCENT_AMBER

    p = ctf_e2.add_paragraph()
    p.text = "Room Ambient Temperature = 27°C"
    p.font.size = Pt(16)
    p.font.bold = True
    p.font.color.rgb = COLOR_TEXT_MAIN

    p = ctf_e2.add_paragraph()
    p.text = "• Classical Thermostat: Turns On 100% or Off 0%. Results in freezing/sweating cycles.\n• Fuzzy Inverter Controller:\n  - μ_Pleasant(27°C) = 0.35\n  - μ_Warm(27°C) = 0.65\n  - μ_Hot(27°C) = 0.10\nOutput: Sets AC compressor speed smoothly to 45% capacity. Quiet, energy-efficient!"
    p.font.size = Pt(11)
    p.font.color.rgb = COLOR_TEXT_MAIN

    s4.notes_slide.notes_text_frame.text = (
        "Explain the graph on Poster 1. Draw attention to how the triangle curves overlap. "
        "At 5'10\", you touch both the 'Average' curve and the 'Tall' curve. This dual membership "
        "is what enables smart systems to blend multiple rules together smoothly."
    )

    # ==========================================
    # SLIDE 5: POSTER 2 — SYĀDVĀDA & ANEKĀNTAVĀDA
    # ==========================================
    s5 = prs.slides.add_slide(blank_layout)
    add_header(s5, "Poster 2: The Philosophical Link — Syādvāda & Anekāntavāda", "Poster 2 Breakdown • Ancient Indian Roots")

    c_cards = [
        ("ANEKĀNTAVĀDA", "The Doctrine of Many-Sidedness", [
            "Root: 'Anek' (many) + 'Anta' (aspects/facets) + 'Vāda' (doctrine).",
            "Foundational principle of Jain philosophy formalized by Bhagwan Mahavira (6th century BCE).",
            "Ultimate reality is infinite in dimensions; no single limited perspective can claim absolute monopoly on truth.",
            "Core Lesson: Intellectual humility, peace, and tolerance of opposing viewpoints."
        ], COLOR_PRIMARY),
        ("THE BLIND MEN & THE ELEPHANT", "The Classic Parable of Partial Perspectives", [
            "6 blind scholars touch an elephant: ear (fan), leg (pillar), tusk (spear), tail (rope), trunk (tree branch), belly (wall).",
            "Each scholar was 100% convinced the others were wrong.",
            "Synthesizer's insight: All were partially correct relative to their point of contact; none possessed the full elephant.",
            "Direct metaphor for fuzzy degrees of truth from partial data."
        ], COLOR_ACCENT_AMBER),
        ("SYĀDVĀDA", "The Logic of Conditional Truth ('Syāt')", [
            "Syāt (स्यात्) = 'In some respect', 'From a certain point of view', 'Conditionally'.",
            "All empirical statements are valid only under specific contextual frames (Dravya, Kshetra, Kāla, Bhāva).",
            "Anticipated Lotfi Zadeh's Principle of Incompatibility by 2,500 years!",
            "Replaces dogmatic 'IS' with contextual 'IS IN A CERTAIN SENSE'."
        ], COLOR_ACCENT_TEAL)
    ]

    card_w = Inches(3.64)
    gap = Inches(0.3)
    for i, (tag, title, bullets, col) in enumerate(c_cards):
        x = Inches(0.8) + i * (card_w + gap)
        c = create_card(s5, x, Inches(1.6), card_w, Inches(5.3), bg_color=COLOR_CARD_LIGHT)
        
        bar = s5.shapes.add_shape(MSO_SHAPE.RECTANGLE, x, Inches(1.6), card_w, Inches(0.12))
        bar.fill.solid()
        bar.fill.fore_color.rgb = col
        bar.line.fill.background()

        ctf = c.text_frame
        ctf.word_wrap = True
        ctf.margin_top = Inches(0.25)
        ctf.margin_left = ctf.margin_right = Inches(0.22)

        ptag = ctf.paragraphs[0]
        ptag.text = tag
        ptag.font.size = Pt(11)
        ptag.font.bold = True
        ptag.font.color.rgb = col

        pt = ctf.add_paragraph()
        pt.text = title
        pt.font.size = Pt(15)
        pt.font.bold = True
        pt.font.color.rgb = COLOR_TEXT_MAIN

        for b in bullets:
            pb = ctf.add_paragraph()
            pb.text = "• " + b
            pb.font.size = Pt(11)
            pb.font.color.rgb = COLOR_TEXT_MUTED

    s5.notes_slide.notes_text_frame.text = (
        "This is the heart of Poster 2! Explain that long before Western science developed multi-valued and fuzzy logic in the 20th century, "
        "Indian philosophers developed Syādvāda. 'Syāt' does not mean doubtful or uncertain—it means rigorously conditional. "
        "Just as an object can be 'warm' relative to ice water but 'cool' relative to boiling water, truth depends on perspective."
    )

    # ==========================================
    # SLIDE 6: POSTER 2 — THE SEVENFOLD PREDICATION (SAPTABHAṄGĪ)
    # ==========================================
    s6 = prs.slides.add_slide(blank_layout)
    add_header(s6, "Poster 2: The Sevenfold Predication (Saptabhaṅgī) Decoded", "Jain Multi-Valued Epistemic Framework")

    # Table layout for 7 predications
    rows = [
        ("1. Syād-asti", "In some respect, it is", "Affirmation (Truth = 1)", "A clay pot exists in terms of its own substance (clay), place, and present time."),
        ("2. Syān-nāsti", "In some respect, it is not", "Negation (Truth = 0)", "The pot does not exist in terms of metal, or in a different city or future time."),
        ("3. Syād-asti-nāsti", "In some respect, it is and is not", "Sequential Duality", "Looking at it consecutively: it is clay now, but not what it used to be."),
        ("4. Syād-avaktavyaḥ", "In some respect, it is inexpressible", "Indeterminate / Indescribable", "Simultaneous assertion of existence and non-existence defies binary language."),
        ("5. Syād-asti-avaktavyaḥ", "In some respect, it is and inexpressible", "Affirmation + Indeterminate", "It exists in its own form, yet its simultaneous total state is inexpressible."),
        ("6. Syān-nāsti-avaktavyaḥ", "In some respect, it is not & inexpressible", "Negation + Indeterminate", "It does not exist in another form, yet its simultaneous state is inexpressible."),
        ("7. Syād-asti-nāsti-avaktavyaḥ", "In some respect, it is, is not, & inexpressible", "Comprehensive Synthesis", "Acknowledging presence, absence, and lingual limits all at once.")
    ]

    # Create table
    table_shape = s6.shapes.add_table(8, 4, Inches(0.8), Inches(1.6), Inches(11.7), Inches(5.3))
    t = table_shape.table
    t.columns[0].width = Inches(2.2)
    t.columns[1].width = Inches(2.8)
    t.columns[2].width = Inches(2.2)
    t.columns[3].width = Inches(4.5)

    headers = ["PREDICATION (SANSKRIT)", "MEANING (PERSPECTIVE)", "LOGIC / COMPUTING ANALOGY", "EVERYDAY CONCRETE EXAMPLE"]
    for col_idx, h in enumerate(headers):
        cell = t.cell(0, col_idx)
        cell.fill.solid()
        cell.fill.fore_color.rgb = COLOR_PRIMARY
        p = cell.text_frame.paragraphs[0]
        p.text = h
        p.font.size = Pt(11)
        p.font.bold = True
        p.font.color.rgb = COLOR_TEXT_LIGHT

    for row_idx, data in enumerate(rows):
        bg = COLOR_CARD_LIGHT if row_idx % 2 == 0 else RGBColor(241, 245, 249)
        for col_idx, text in enumerate(data):
            cell = t.cell(row_idx + 1, col_idx)
            cell.fill.solid()
            cell.fill.fore_color.rgb = bg
            p = cell.text_frame.paragraphs[0]
            p.text = text
            p.font.size = Pt(10)
            if col_idx == 0:
                p.font.bold = True
                p.font.color.rgb = COLOR_PRIMARY
            elif col_idx == 2:
                p.font.bold = True
                p.font.color.rgb = COLOR_ACCENT_TEAL
            else:
                p.font.color.rgb = COLOR_TEXT_MAIN

    s6.notes_slide.notes_text_frame.text = (
        "Highlight to judges that Saptabhaṅgī is the world's first formal 7-valued logic system! "
        "Show how row 4 introduces 'Avaktavya' (indescribable / fuzzy boundary state), which classical binary logic completely ignores. "
        "This proves ancient Indian philosophy was centuries ahead in modeling ambiguity and contextual truth."
    )

    # ==========================================
    # SLIDE 7: WORKING MODEL 1 — CHART PAPER WITH MOVABLE POINTERS
    # ==========================================
    s7 = prs.slides.add_slide(blank_layout)
    add_header(s7, "Working Model 1: Physical Chart Paper with Movable Pointers", "Hands-on Physical Demonstration")

    c_build = create_card(s7, Inches(0.8), Inches(1.6), Inches(4.5), Inches(5.3), bg_color=COLOR_CARD_LIGHT)
    ctf_b = c_build.text_frame
    ctf_b.word_wrap = True
    ctf_b.margin_left = ctf_b.margin_right = ctf_b.margin_top = Inches(0.3)

    p = ctf_b.paragraphs[0]
    p.text = "HOW TO CRAFT THE MODEL"
    p.font.size = Pt(12)
    p.font.bold = True
    p.font.color.rgb = COLOR_PRIMARY

    p = ctf_b.add_paragraph()
    p.text = "DIY Interactive Chart Board"
    p.font.size = Pt(17)
    p.font.bold = True
    p.font.color.rgb = COLOR_TEXT_MAIN

    craft_steps = [
        ("Materials", "1 Foam core board (A2 size), 3 colored cardstock sheets, 1 clear acrylic ruler / slider strip, 2 brass split pins."),
        ("Graph Setup", "Draw X-axis: Height in Feet (4'6\" to 6'6\") or Temp (0°C to 45°C).\nDraw Y-axis: Degree of Membership (0.0 to 1.0)."),
        ("Overlapping Curves", "Paste 3 colored paper curves:\n• Blue Triangle: 'Short'\n• Green Trapezoid: 'Average'\n• Orange Triangle: 'Tall'"),
        ("The Movable Pointer", "Mount a vertical sliding clear cursor that moves horizontally across heights, with horizontal indicator lines pointing to Y-axis truth values.")
    ]
    for h, d in craft_steps:
        ph = ctf_b.add_paragraph()
        ph.text = "✔ " + h
        ph.font.size = Pt(12)
        ph.font.bold = True
        ph.font.color.rgb = COLOR_PRIMARY
        pd = ctf_b.add_paragraph()
        pd.text = d
        pd.font.size = Pt(10)
        pd.font.color.rgb = COLOR_TEXT_MUTED

    # Right Side: The Demonstration Experience
    c_exp = create_card(s7, Inches(5.6), Inches(1.6), Inches(6.9), Inches(5.3), bg_color=COLOR_PRIMARY_LIGHT, border_color=COLOR_PRIMARY)
    ctf_x = c_exp.text_frame
    ctf_x.word_wrap = True
    ctf_x.margin_left = ctf_x.margin_right = ctf_x.margin_top = Inches(0.3)

    p = ctf_x.paragraphs[0]
    p.text = "THE VISITOR DEMONSTRATION SCRIPT"
    p.font.size = Pt(12)
    p.font.bold = True
    p.font.color.rgb = COLOR_PRIMARY

    p = ctf_x.add_paragraph()
    p.text = "Live Interaction at the Booth (3 Steps)"
    p.font.size = Pt(18)
    p.font.bold = True
    p.font.color.rgb = COLOR_TEXT_MAIN

    exp_steps = [
        ("Step 1: Invite Visitor to Slide the Pointer", "Ask: 'What is your height? Slide this pointer to 5'9\" (175 cm).'"),
        ("Step 2: Read the Dual Truth Values", "Point to the two intersection points:\n- The slider cuts the Green 'Average' curve at 0.50.\n- The slider cuts the Orange 'Tall' curve at 0.40.\nExplain: 'In binary logic, you'd be forced into one box. Here, you belong to both sets simultaneously!'"),
        ("Step 3: The Syādvāda Connection", "Explain: 'Syād-asti (From the viewpoint of someone 5'2\", you are tall). Syān-nāsti (From the viewpoint of a 6'5\" basketball player, you are not tall). Both are valid within their frame!'")
    ]
    for h, d in exp_steps:
        ph = ctf_x.add_paragraph()
        ph.text = h
        ph.font.size = Pt(14)
        ph.font.bold = True
        ph.font.color.rgb = COLOR_PRIMARY
        pd = ctf_x.add_paragraph()
        pd.text = d
        pd.font.size = Pt(12)
        pd.font.color.rgb = COLOR_TEXT_MAIN

    s7.notes_slide.notes_text_frame.text = (
        "Demonstrate the chart board. Physically take the slider and place it at 5'9\". "
        "Show how the pointer touches two colors at once. This single visual interaction instantly demystifies fuzzy logic "
        "for students, teachers, and judges."
    )

    # ==========================================
    # SLIDE 8: WORKING MODEL 2 — FUZZY CONTROLLER SIMULATION
    # ==========================================
    s8 = prs.slides.add_slide(blank_layout)
    add_header(s8, "Working Model 2: Fuzzy Logic Machine Simulations", "Engineering Simulation (Python / Microcontroller)")

    # Sim 1: Washing machine
    c_wm = create_card(s8, Inches(0.8), Inches(1.6), Inches(5.6), Inches(5.3), bg_color=COLOR_CARD_LIGHT, border_color=COLOR_ACCENT_TEAL)
    ctf_wm = c_wm.text_frame
    ctf_wm.word_wrap = True
    ctf_wm.margin_left = ctf_wm.margin_right = ctf_wm.margin_top = Inches(0.3)

    p = ctf_wm.paragraphs[0]
    p.text = "SIMULATION A: FUZZY WASHING MACHINE"
    p.font.size = Pt(12)
    p.font.bold = True
    p.font.color.rgb = COLOR_ACCENT_TEAL

    p = ctf_wm.add_paragraph()
    p.text = "Optimal Wash Time & Water Level"
    p.font.size = Pt(16)
    p.font.bold = True
    p.font.color.rgb = COLOR_TEXT_MAIN

    wm_items = [
        ("Sensors / Inputs", "• Dirtiness of wash water (Optical sensor: Clean, Medium, Muddy)\n• Fabric Type / Weight (Load sensor: Silk, Cotton, Heavy Denim)"),
        ("Fuzzy Rule Inference (Mamdani)", "• IF Dirt is Muddy AND Fabric is Heavy THEN Wash Time is 60 min.\n• IF Dirt is Clean AND Fabric is Silk THEN Wash Time is 15 min.\n• IF Dirt is Medium AND Fabric is Cotton THEN Wash Time is 35 min."),
        ("Defuzzification (Centroid Method)", "Calculates the exact minute-level wash duration (e.g. 38.4 minutes) instead of rigid 15/30/60 presets."),
        ("Real Benefit", "Saves 30% water and 25% electricity while protecting fragile clothes.")
    ]
    for h, d in wm_items:
        ph = ctf_wm.add_paragraph()
        ph.text = "• " + h
        ph.font.size = Pt(12)
        ph.font.bold = True
        ph.font.color.rgb = COLOR_ACCENT_TEAL
        pd = ctf_wm.add_paragraph()
        pd.text = d
        pd.font.size = Pt(11)
        pd.font.color.rgb = COLOR_TEXT_MUTED

    # Sim 2: Temperature controller
    c_tc = create_card(s8, Inches(6.8), Inches(1.6), Inches(5.6), Inches(5.3), bg_color=COLOR_CARD_LIGHT, border_color=COLOR_ACCENT_AMBER)
    ctf_tc = c_tc.text_frame
    ctf_tc.word_wrap = True
    ctf_tc.margin_left = ctf_tc.margin_right = ctf_tc.margin_top = Inches(0.3)

    p = ctf_tc.paragraphs[0]
    p.text = "SIMULATION B: SMART FUZZY THERMOSTAT"
    p.font.size = Pt(12)
    p.font.bold = True
    p.font.color.rgb = COLOR_ACCENT_AMBER

    p = ctf_tc.add_paragraph()
    p.text = "Inverter AC / Fan Speed Controller"
    p.font.size = Pt(16)
    p.font.bold = True
    p.font.color.rgb = COLOR_TEXT_MAIN

    tc_items = [
        ("Sensors / Inputs", "• Temperature Error = (Current Temp - Target Temp)\n• Rate of Change = How quickly the room is warming up (ΔT/Δt)"),
        ("Fuzzy Control Rules", "• IF Error is Small Positive AND Rate is Zero THEN Fan Speed is Low.\n• IF Error is Large Positive AND Rate is Warming Fast THEN Fan Speed is Maximum.\n• IF Error is Zero THEN Maintain Minimal Idle Speed."),
        ("Continuous Inverter Output", "Rather than switching compressor violently On and Off, it smoothly throttles power from 20% to 100%."),
        ("Real Benefit", "Maintains exact room temperature within ±0.2°C without disturbing clicking noises.")
    ]
    for h, d in tc_items:
        ph = ctf_tc.add_paragraph()
        ph.text = "• " + h
        ph.font.size = Pt(12)
        ph.font.bold = True
        ph.font.color.rgb = COLOR_ACCENT_AMBER
        pd = ctf_tc.add_paragraph()
        pd.text = d
        pd.font.size = Pt(11)
        pd.font.color.rgb = COLOR_TEXT_MUTED

    s8.notes_slide.notes_text_frame.text = (
        "Explain how simple rules written in natural English ('IF dirt is medium THEN wash is medium') "
        "translate into precise mathematical control. This can be demonstrated with our accompanying Python simulator "
        "or paper dials that let visitors adjust Dirtiness and read the calculated Wash Time."
    )

    # ==========================================
    # SLIDE 9: INTERACTIVE ACTIVITY FOR VISITORS
    # ==========================================
    s9 = prs.slides.add_slide(blank_layout)
    add_header(s9, "Interactive Activity: 'The Human Fuzzy Set' Live Experiment", "Booth Visitor Engagement Plan")

    c_act = create_card(s9, Inches(0.8), Inches(1.6), Inches(11.7), Inches(5.3), bg_color=COLOR_CARD_LIGHT)
    ctf_a = c_act.text_frame
    ctf_a.word_wrap = True
    ctf_a.margin_left = ctf_a.margin_right = ctf_a.margin_top = Inches(0.3)

    p = ctf_a.paragraphs[0]
    p.text = "VISITOR VOTING & RATING STATION"
    p.font.size = Pt(12)
    p.font.bold = True
    p.font.color.rgb = COLOR_PRIMARY

    p = ctf_a.add_paragraph()
    p.text = "How to Run the Live Crowd-Sourced Fuzzy Distribution"
    p.font.size = Pt(18)
    p.font.bold = True
    p.font.color.rgb = COLOR_TEXT_MAIN

    boxes = [
        ("Station Setup", "Provide a chart board or tablet with 3 continuous sliders (0.0 to 1.0).\nVisitors get 3 colored circular sticker dots.", COLOR_PRIMARY),
        ("Question 1: 'TALL'", "Show photo of a 5'9\" (175 cm) man.\nAsk: 'Rate on a scale from 0.0 (Not tall at all) to 1.0 (Definitely tall).'", COLOR_ACCENT_ROSE),
        ("Question 2: 'SWEET'", "Display a cup of tea with 2 teaspoons of sugar.\nAsk: 'Rate how sweet this is on a 0.0 to 1.0 scale.'", COLOR_ACCENT_AMBER),
        ("Question 3: 'INTELLIGENT'", "Display a student with 88% marks who built a working robot.\nAsk: 'Rate this achievement on a 0.0 to 1.0 scale.'", COLOR_ACCENT_TEAL)
    ]
    
    # 4 Sub-cards inside
    sw = Inches(2.7)
    sgap = Inches(0.2)
    for idx, (head, text, col) in enumerate(boxes):
        sx = Inches(1.1) + idx * (sw + sgap)
        sc = create_card(s9, sx, Inches(2.7), sw, Inches(2.2), bg_color=RGBColor(248, 250, 252), border_color=col)
        sctf = sc.text_frame
        sctf.word_wrap = True
        sctf.margin_top = Inches(0.15)
        sctf.margin_left = sctf.margin_right = Inches(0.15)
        
        sp = sctf.paragraphs[0]
        sp.text = head
        sp.font.size = Pt(11)
        sp.font.bold = True
        sp.font.color.rgb = col
        
        sp2 = sctf.add_paragraph()
        sp2.text = text
        sp2.font.size = Pt(11)
        sp2.font.color.rgb = COLOR_TEXT_MAIN

    # Bottom reveal callout
    reveal_box = create_card(s9, Inches(1.1), Inches(5.1), Inches(11.1), Inches(1.5), bg_color=COLOR_PRIMARY_LIGHT, border_color=COLOR_PRIMARY)
    rtf = reveal_box.text_frame
    rtf.word_wrap = True
    rtf.margin_left = rtf.margin_right = rtf.margin_top = Inches(0.2)

    rp = rtf.paragraphs[0]
    rp.text = "THE 'AHA!' REVEAL FOR VISITORS:"
    rp.font.size = Pt(12)
    rp.font.bold = True
    rp.font.color.rgb = COLOR_PRIMARY

    rp2 = rtf.add_paragraph()
    rp2.text = "At the end of the day, show the board to the judges: the dots naturally form a continuous Gaussian / Fuzzy bell curve! " \
               "No two humans agree on a single binary cutoff. This physical evidence proves that human cognition is inherently fuzzy and multi-perspectival (Syādvāda), not binary."
    rp2.font.size = Pt(12)
    rp2.font.color.rgb = COLOR_TEXT_MAIN

    s9.notes_slide.notes_text_frame.text = (
        "Judges love audience interaction! Explain how every visitor who stops at the booth contributes one data point. "
        "By 12:00 PM, you will have a real-time, empirical fuzzy membership graph generated directly by the audience."
    )

    # ==========================================
    # SLIDE 10: REAL-LIFE APPLICATIONS TABLE
    # ==========================================
    s10 = prs.slides.add_slide(blank_layout)
    add_header(s10, "Real-Life Applications Matrix: Fuzzy Logic in the Wild", "Industrial & Consumer Engineering Applications")

    app_table = s10.shapes.add_table(6, 4, Inches(0.8), Inches(1.6), Inches(11.7), Inches(5.3))
    at = app_table.table
    at.columns[0].width = Inches(2.2)
    at.columns[1].width = Inches(2.8)
    at.columns[2].width = Inches(3.2)
    at.columns[3].width = Inches(3.5)

    app_headers = ["SECTOR", "FAMOUS REAL EXAMPLE", "FUZZY INPUTS & LOGIC", "REAL-WORLD IMPACT"]
    for col_idx, h in enumerate(app_headers):
        cell = at.cell(0, col_idx)
        cell.fill.solid()
        cell.fill.fore_color.rgb = COLOR_ACCENT_TEAL
        p = cell.text_frame.paragraphs[0]
        p.text = h
        p.font.size = Pt(11)
        p.font.bold = True
        p.font.color.rgb = COLOR_TEXT_LIGHT

    app_rows = [
        ("High-Speed Transit", "Sendai Subway System (Japan, Hitachi)", "Speed, Track gradient, Distance to station, Passenger comfort index", "Smooth automatic train acceleration & braking; saves 10% energy, passengers stand without holding rails!"),
        ("Home Appliances", "Panasonic & Hitachi Fuzzy Washing Machines", "Optical dirt turbidity sensor, load weight, fabric friction", "Optimizes wash duration and water levels automatically; no wasted detergent or fabric damage."),
        ("Consumer Cameras", "Sony & Canon Autofocus / Auto-exposure", "Lighting intensity, motion vector blur, edge contrast", "Avoids focus hunting; smoothly tracks faces and stabilizes video in low light."),
        ("Automotive Safety", "Anti-Lock Braking (ABS) & Transmission", "Wheel slip velocity, brake pedal pressure, road surface friction", "Prevents wheel lockup on icy roads; shifts gears smoothly based on driver intent."),
        ("Medical Diagnostics & Cyber Defense", "ICU Patient Monitoring & EdgeGuard Fraud Defense", "Subtle vital sign trends; behavioral anomalies in SMS messages", "Flagging suspicious behavior degrees (e.g. 0.82 phishing probability) instead of missed binary blocks.")
    ]

    for row_idx, data in enumerate(app_rows):
        bg = COLOR_CARD_LIGHT if row_idx % 2 == 0 else RGBColor(240, 253, 250)
        for col_idx, text in enumerate(data):
            cell = at.cell(row_idx + 1, col_idx)
            cell.fill.solid()
            cell.fill.fore_color.rgb = bg
            p = cell.text_frame.paragraphs[0]
            p.text = text
            p.font.size = Pt(10)
            if col_idx == 0:
                p.font.bold = True
                p.font.color.rgb = COLOR_PRIMARY
            elif col_idx == 1:
                p.font.bold = True
                p.font.color.rgb = COLOR_TEXT_MAIN
            else:
                p.font.color.rgb = COLOR_TEXT_MUTED

    s10.notes_slide.notes_text_frame.text = (
        "Point to the Sendai Subway case study—it is the most legendary real-world validation of fuzzy logic in history! "
        "Standard PID controllers caused passengers to lurch forward when stopping. Fuzzy control delivered a ride so smooth "
        "that it revolutionized automated public transit worldwide."
    )

    # ==========================================
    # SLIDE 11: BOOTH SETUP & 60-SECOND PITCH SCRIPT
    # ==========================================
    s11 = prs.slides.add_slide(blank_layout)
    add_header(s11, "Exhibition Layout Blueprint & 60-Second Pitch Script", "How to Present at Your Booth")

    # Left: Physical Booth Layout
    c_booth = create_card(s11, Inches(0.8), Inches(1.6), Inches(5.6), Inches(5.3), bg_color=COLOR_CARD_LIGHT)
    ctf_b = c_booth.text_frame
    ctf_b.word_wrap = True
    ctf_b.margin_left = ctf_b.margin_right = ctf_b.margin_top = Inches(0.3)

    p = ctf_b.paragraphs[0]
    p.text = "PHYSICAL BOOTH LAYOUT (TRI-FOLD DISPLAY)"
    p.font.size = Pt(12)
    p.font.bold = True
    p.font.color.rgb = COLOR_PRIMARY

    p = ctf_b.add_paragraph()
    p.text = "Zoning Your Presentation Table"
    p.font.size = Pt(16)
    p.font.bold = True
    p.font.color.rgb = COLOR_TEXT_MAIN

    booth_zones = [
        ("Left Wall — Poster 1: The Intuitive Hook", "Everyday examples ('Is he tall?'), crisp vs. fuzzy graphs, large colorful bell curves."),
        ("Right Wall — Poster 2: The Philosophy Anchor", "Syādvāda, Anekāntavāda, Elephant Parable illustration, Saptabhaṅgī 7-fold table."),
        ("Center Table — The Working Model", "Chart paper board with physical sliding pointer + Laptop showing interactive Python simulator."),
        ("Front Counter — Visitor Activity", "Sticker dot voting board ('How tall / sweet is this?'), handouts summarizing key takeaways.")
    ]
    for h, d in booth_zones:
        ph = ctf_b.add_paragraph()
        ph.text = "📍 " + h
        ph.font.size = Pt(12)
        ph.font.bold = True
        ph.font.color.rgb = COLOR_PRIMARY
        pd = ctf_b.add_paragraph()
        pd.text = d
        pd.font.size = Pt(11)
        pd.font.color.rgb = COLOR_TEXT_MUTED

    # Right: The Pitch Script
    c_pitch = create_card(s11, Inches(6.8), Inches(1.6), Inches(5.6), Inches(5.3), bg_color=RGBColor(245, 243, 255), border_color=COLOR_PRIMARY)
    ctf_p = c_pitch.text_frame
    ctf_p.word_wrap = True
    ctf_p.margin_left = ctf_p.margin_right = ctf_p.margin_top = Inches(0.3)

    p = ctf_p.paragraphs[0]
    p.text = "THE 60-SECOND JUDGE PITCH SCRIPT"
    p.font.size = Pt(12)
    p.font.bold = True
    p.font.color.rgb = COLOR_PRIMARY

    p = ctf_p.add_paragraph()
    p.text = "Memorize & Deliver with Confidence"
    p.font.size = Pt(16)
    p.font.bold = True
    p.font.color.rgb = COLOR_TEXT_MAIN

    script = (
        "\"Good morning, judges! In standard computing, everything is forced into 0 or 1—True or False. "
        "But if I ask you, 'Is 26°C hot?', or 'Is someone 5'10\" tall?', rigid binary logic fails completely.\n\n"
        "In 1965, Lotfi Zadeh introduced Fuzzy Logic to give computers degrees of truth. But 2,500 years ago, "
        "Indian philosophy developed Syādvāda—the doctrine of conditional truth—asserting that truth depends on perspective.\n\n"
        "Our project brings this to life: here on Poster 1, we contrast crisp thresholds with smooth membership curves. "
        "On Poster 2, we map the 7-fold Saptabhaṅgī logic into computer science. "
        "And here on our working model, you can slide this pointer yourself to see dual membership in action, or test our "
        "smart fuzzy appliance simulator. Let me show you how it works!\""
    )
    p = ctf_p.add_paragraph()
    p.text = script
    p.font.size = Pt(11)
    p.font.color.rgb = COLOR_TEXT_MAIN

    s11.notes_slide.notes_text_frame.text = (
        "Practice this 60-second pitch. Keep eye contact, smile, and gesture toward the physical slider when mentioning 'smooth membership curves'. "
        "It establishes you as an expert who understands both the math and the philosophy."
    )

    # ==========================================
    # SLIDE 12: CONCLUSION & SUMMARY
    # ==========================================
    s12 = prs.slides.add_slide(blank_layout)
    bg12 = s12.shapes.add_shape(MSO_SHAPE.RECTANGLE, 0, 0, prs.slide_width, prs.slide_height)
    bg12.fill.solid()
    bg12.fill.fore_color.rgb = COLOR_BG_DARK
    bg12.line.fill.background()

    add_header(s12, "Synthesis: Why Nuance Matters in AI & Humanity", "Conclusion & Key Takeaways", dark=True)

    concl_cards = [
        ("ENGINEERING LESSON", "Binary vs. Fuzzy Reality", 
         "Binary logic is ideal for hardware switches, but catastrophic for modeling human language, emotion, and perception. "
         "Fuzzy logic provides the essential bridge.", COLOR_ACCENT_ROSE),
        ("PHILOSOPHICAL HERITAGE", "Indian Epistemic Genius", 
         "Syādvāda and Anekāntavāda prove that ancient Indian philosophy was deeply analytical and pluralistic, anticipating modern multi-valued logic by millennia.", COLOR_ACCENT_AMBER),
        ("THE FUTURE OF AI", "Ethical & Nuanced Technology", 
         "As AI systems interact with society, we must move beyond rigid binary verdicts. Safe, compassionate AI requires degrees of certainty and perspective.", COLOR_ACCENT_TEAL)
    ]

    cw = Inches(3.64)
    cgap = Inches(0.3)
    for i, (tag, head, text, col) in enumerate(concl_cards):
        cx = Inches(0.8) + i * (cw + cgap)
        c = create_card(s12, cx, Inches(1.8), cw, Inches(4.5), bg_color=COLOR_CARD_DARK, border_color=col)
        
        cbar = s12.shapes.add_shape(MSO_SHAPE.RECTANGLE, cx, Inches(1.8), cw, Inches(0.12))
        cbar.fill.solid()
        cbar.fill.fore_color.rgb = col
        cbar.line.fill.background()

        ctf = c.text_frame
        ctf.word_wrap = True
        ctf.margin_top = Inches(0.3)
        ctf.margin_left = ctf.margin_right = Inches(0.25)

        ptag = ctf.paragraphs[0]
        ptag.text = tag
        ptag.font.size = Pt(11)
        ptag.font.bold = True
        ptag.font.color.rgb = col

        phead = ctf.add_paragraph()
        phead.text = head
        phead.font.size = Pt(17)
        phead.font.bold = True
        phead.font.color.rgb = COLOR_TEXT_LIGHT

        pdesc = ctf.add_paragraph()
        pdesc.text = text
        pdesc.font.size = Pt(13)
        pdesc.font.color.rgb = RGBColor(203, 213, 225)

    # Thank you footer
    tf_box = s12.shapes.add_textbox(Inches(0.8), Inches(6.5), Inches(11.7), Inches(0.5))
    ttf = tf_box.text_frame
    tp = ttf.paragraphs[0]
    tp.text = "Thank you! Open for Questions & Hands-on Demonstrations."
    tp.font.size = Pt(14)
    tp.font.bold = True
    tp.font.color.rgb = RGBColor(165, 180, 252)
    tp.alignment = PP_ALIGN.CENTER

    s12.notes_slide.notes_text_frame.text = (
        "Wrap up your presentation by inviting the judges to ask questions and try the interactive models. "
        "Reiterate that this project proves that ancient philosophy and modern computing are two sides of the same human quest for truth."
    )

    prs.save(filename)
    print(f"Successfully generated presentation: {filename}")

if __name__ == "__main__":
    create_presentation()

