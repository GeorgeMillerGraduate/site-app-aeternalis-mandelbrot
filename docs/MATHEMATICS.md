# Numerical notes

For a pixel centre c, the quadratic Mandelbrot iteration starts with z₀ = 0 and applies zₙ₊₁ = zₙ² + c. Writing z = a + bi and c = x + yi gives a′ = a² − b² + x and b′ = 2ab + y. The engine stops when |z|² > 4, or when the chosen limit is reached. The escape test occurs after the final update as well, so an orbit escaping on its last permitted iteration is still exterior.

The standard cardioid condition uses q = (x − 1/4)² + y² and q(q + x − 1/4) < y²/4. The period-two bulb satisfies (x + 1)² + y² < 1/16. This version moves both comparisons inward by 10⁻¹⁴ rather than trusting rounded equality at the boundary. These optimisations are quadratic-Mandelbrot-specific.

Smooth exterior values use μ = k + 1 − log(log |zₖ|)/log 2. Here k includes up to three additional finite orbit steps after first escape, improving the asymptotic approximation without changing whether the point escaped within the original iteration limit. The telemetry counts escape iterations, excluding these extra colour-refinement steps.

## Cubic gradient and transfer mapping

For two stops at positions a and b, normalize t=(position−a)/(b−a), then blend with h(t)=3t²−2t³. The result interpolates the chosen endpoint colours exactly, with zero first derivative at each stop and no overshoot. RGB channels are converted from sRGB to linear light before this cubic blend.

The colour coordinate starts at u=max(0,μ/escapeScale). A transfer f(u) is chosen from u, sqrt(u), cbrt(u), log1p(u) or u^power. Apply density × f(u) + offset, then repeat using the fractional part or clamp to [0,1], and optionally reverse. Square root compresses high escape values; it is a colour mapping, not a change to the Mandelbrot recurrence. The Linear transfer option does not change the cubic gradient interpolation.

With histogram mode, a 4096-bin exterior CDF replaces μ/escapeScale as the input to the transfer. Interior samples are excluded. Empty or uniform exterior data use a stable midpoint fallback. A full colour pass is based on one immutable tile snapshot so it does not mix partial histogram generations.

Anti-aliasing evaluates regular subpixel centres. Each subpixel receives its own final linear-light colour; these colours are averaged before sRGB encoding. Averaging escape values before applying a nonlinear palette would be a different, incorrect operation. The cached subpixel values permit new palettes without repeating orbits. Uniform supersampling reduces aliasing but cannot guarantee capturing arbitrarily thin structures.

## Coordinates and precision

Horizontal span S and physical image width W yield square complex-plane pixels of size S/W. A pixel's imaginary coordinate decreases down the image. Draft samples x+1/2 and y+1/2; an m × m grid samples x+(i+1/2)/m and y+(j+1/2)/m. Cursor-centred zoom calculates the anchor first, then moves the centre towards that anchor while scaling S.

IEEE-754 double has 53 significant binary digits. The current engine automatically escalates through DD, QD and arbitrary precision when the runtime coordinate registry observes distinct axis sample positions mapping to the same represented value. Exact decimal navigation prevents double-rounding away the requested view. See PRECISION-AND-HARDWARE.md for numerical boundaries and perturbation safeguards.

## Legacy cycle and curve additions

The accelerated evaluator stores a reference state at an adaptive interval and a spaced state after iteration 10. It compares both real and imaginary double components exactly after checking bailout. Returning to the same double state repeats the deterministic floating-point recurrence; near-equality is not enough. A toggle allows the direct finite evaluator without these checks. Analytic interior shortcuts remain independent.

Catmull–Rom is available alongside smoothstep. On an interval [a,b], linear-light channel values p₁,p₂ are blended using Hermite basis functions. Tangents use neighbouring stop differences divided by their position separation, scaled by b−a. With equally spaced interior stops this is the old four-point Catmull–Rom cubic. Endpoints use repeated end values and half one-sided slopes. Channels are clamped into [0,1], so extreme gradients can flatten at the gamut boundary.

Rectangle zoom uses a display-space box expanded to the viewport aspect ratio and clamped inside the image. Its centre maps into the current complex plane and its width fraction scales the horizontal span. Both axes use the physical-output/display-coordinate conversion, including high-DPI scaling.

## Deep-zoom execution

DD uses a hi+lo expansion with TwoSum and FMA residuals; QD retains four compressed components. BigDecimal provides variable decimal precision. Pixel mapping and iteration both use the selected representation. For compatible Mandelbrot views, dz' = 2Z dz + dz² + dc uses a high-precision reference, propagated error checks and arbitrary-precision fallback. Finite computations and iteration limits do not prove infinite membership. Series approximation and FloatExp perturbation are not implemented.

See ../benchmarks/BENCHMARKS.md for measured rendering timings and methodology.

## Julia, Newton and composition

Julia uses the pixel as z₀ and a fixed c, with a conservative escape threshold for larger constants. It uses quadratic iteration. Newton now applies z ← z − (zⁿ−1)/(n zⁿ⁻¹) for every integer n from 2 through 12. All recurrence operations use the selected numerical backend. Residual modulus below 1e-10 marks convergence; the nearest unit-root sector selects the colour band, with iteration-dependent variation. A singular derivative/non-converged sample is interior-style. Newton convergence is not an escape event. Mandelbrot-only analytic/cycle shortcuts are never applied to Julia or Newton. Burning Ship, Tricorn and Multibrot are removed in 0.9.

A transparent interior contributes zero alpha; a coloured subpixel contributes alpha one. Average premultiplied colour and coverage over the subpixels, then divide by coverage to obtain straight RGB for storage. For a source with alpha aₛ and backdrop alpha aᵦ, output alpha is aₛ+aᵦ(1−aₛ). For straight channel values Cₛ,Cᵦ and blend B, premultiplied output is aₛ(1−aᵦ)Cₛ + aₛaᵦB(Cᵦ,Cₛ) + (1−aₛ)aᵦCᵦ. Divide by output alpha when nonzero. Blending takes place in linear light; the stored intermediate layers remain 8-bit ARGB.
